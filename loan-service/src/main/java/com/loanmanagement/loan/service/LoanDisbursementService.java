package com.loanmanagement.loan.service;

import com.loanmanagement.common.event.DomainEvent;
import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.common.event.LoanEvents;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.domain.LoanStateMachine;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.outbox.OutboxService;
import com.loanmanagement.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanDisbursementService {

    private final LoanRepository loanRepository;
    private final LoanStateMachine stateMachine;
    private final OutboxService outboxService;
    private final LoanApplicationService applicationService;

    @Transactional
    public LoanResponseDto disburse(Long loanId, String idempotencyKey, String disbursedBy) {
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = loanRepository.findByDisbursementIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent hit for key {} → returning existing loan {}", idempotencyKey, loanId);
                return applicationService.getById(existing.get().getLoanId());
            }
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        stateMachine.validateTransition(loan.getLoanStatus(), LoanStatus.DISBURSED);

        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();

        if (loanRepository.existsByDisbursementIdempotencyKey(key)) {
            throw new DomainException("Idempotency key already used", HttpStatus.CONFLICT);
        }

        loan.setLoanStatus(LoanStatus.DISBURSED);
        loan.setDisbursementDate(LocalDate.now());
        loan.setDisbursementIdempotencyKey(key);
        loan.setOutstandingPrincipal(loan.getLoanAmount());
        loan.setRemainingInstallments(loan.getTenureMonths());
        loan.setNextDueDate(LocalDate.now().plusMonths(1));

        // Immediately move to ACTIVE after disbursement (as per original lifecycle)
        stateMachine.validateTransition(LoanStatus.DISBURSED, LoanStatus.ACTIVE);
        loan.setLoanStatus(LoanStatus.ACTIVE);

        loan = loanRepository.save(loan);

        LoanEventPayload payload = LoanEventPayload.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .status(LoanStatus.ACTIVE.name())
                .previousStatus(LoanStatus.APPROVED.name())
                .loanAmount(loan.getLoanAmount())
                .emi(loan.getEmi())
                .disbursementDate(loan.getDisbursementDate())
                .approvedBy(disbursedBy)
                .build();

        outboxService.enqueue(DomainEvent.of(
                LoanEvents.LOAN_DISBURSED,
                loan.getLoanId().toString(),
                "Loan",
                payload
        ));

        log.info("Loan {} disbursed by {} (key={})", loanId, disbursedBy, key);
        return applicationService.getById(loan.getLoanId());
    }
}
