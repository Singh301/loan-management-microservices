package com.loanmanagement.loan.service;

import com.loanmanagement.common.event.DomainEvent;
import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.common.event.LoanEvents;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.domain.LoanStateMachine;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.idempotency.IdempotencyService;
import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.outbox.OutboxService;
import com.loanmanagement.loan.repository.LoanRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains managed loan service beans.")
public class LoanDisbursementService {

    private final LoanRepository loanRepository;
    private final LoanStateMachine stateMachine;
    private final OutboxService outboxService;
    private final LoanApplicationService applicationService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public LoanResponseDto disburse(Long loanId, String idempotencyKey, String disbursedBy) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();
        boolean clientProvidedIdempotencyKey =
                idempotencyKey != null && !idempotencyKey.isBlank();

        if (clientProvidedIdempotencyKey && (key.length() < 8 || key.length() > 100)) {
            throw new DomainException(
                    "Idempotency-Key must contain between 8 and 100 characters",
                    HttpStatus.BAD_REQUEST);
        }

        // Keep the legacy loan-column lookup for records created before the shared idempotency ledger.
        if (clientProvidedIdempotencyKey) {
            var legacy = loanRepository.findByDisbursementIdempotencyKey(key);
            if (legacy.isPresent()) {
                log.info("Idempotent hit for legacy disbursement key {}", key);
                return applicationService.getById(legacy.get().getLoanId());
            }
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));

        stateMachine.validateTransition(loan.getLoanStatus(), LoanStatus.DISBURSED);

        String requestHash = null;
        if (clientProvidedIdempotencyKey) {
            requestHash = idempotencyService.requestHash(
                    IdempotencyService.DISBURSE_OPERATION + ":" + loanId);

            LoanResponseDto existing = idempotencyService.findExisting(
                    key, requestHash, IdempotencyService.DISBURSE_OPERATION);
            if (existing != null) {
                log.info("Idempotent hit for disbursement key {}", key);
                return existing;
            }

            idempotencyService.claim(key, requestHash, IdempotencyService.DISBURSE_OPERATION);
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

        LoanResponseDto response = applicationService.getById(loan.getLoanId());

        if (clientProvidedIdempotencyKey) {
            idempotencyService.complete(key, response, IdempotencyService.DISBURSE_OPERATION);
        }

        log.info("Loan {} disbursed by {} (key={})", loanId, disbursedBy,
                clientProvidedIdempotencyKey ? key : "generated");
        return response;
    }
}
