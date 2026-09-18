package com.loanmanagement.loan.service;

import com.loanmanagement.common.event.DomainEvent;
import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.common.event.LoanEvents;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.domain.LoanStateMachine;
import com.loanmanagement.loan.dto.LoanRequestDto;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.idempotency.IdempotencyService;
import com.loanmanagement.loan.outbox.OutboxService;
import com.loanmanagement.loan.repository.LoanRepository;
import com.loanmanagement.loan.util.EmiCalculator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains managed loan service beans.")
public class LoanApplicationService {
    private final LoanRepository loanRepository;
    private final LoanStateMachine stateMachine;
    private final OutboxService outboxService;
    private final EmiCalculator emiCalculator;
    private final CustomerValidationService customerValidationService;
    private final IdempotencyService idempotencyService;

    @Transactional
    public LoanResponseDto apply(
            LoanRequestDto request,
            String idempotencyKey,
            String requestHash) {
        idempotencyService.claim(idempotencyKey, requestHash);
        validateCustomer(request.getCustomerId());

        Loan loan = Loan.builder()
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .loanType(request.getLoanType())
                .loanAmount(request.getLoanAmount())
                .interestRate(request.getInterestRate())
                .tenureMonths(request.getTenureMonths())
                .remarks(request.getRemarks())
                .loanStatus(LoanStatus.PENDING)
                .outstandingPrincipal(request.getLoanAmount())
                .remainingInstallments(request.getTenureMonths())
                .build();

        loan = loanRepository.save(loan);

        LoanEventPayload payload = LoanEventPayload.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .status(loan.getLoanStatus().name())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .applicationDate(loan.getApplicationDate())
                .build();

        outboxService.enqueue(
                DomainEvent.of(
                        LoanEvents.LOAN_APPLIED,
                        loan.getLoanId().toString(),
                        "Loan",
                        payload));

        LoanResponseDto response = toDto(loan);
        idempotencyService.complete(idempotencyKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public LoanResponseDto getById(Long loanId) {
        return toDto(findLoan(loanId));
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> getByCustomer(
            Long customerId,
            Pageable pageable) {
        return loanRepository.findByCustomerId(customerId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public LoanResponseDto approveLevel1(
            Long loanId,
            String approvedBy,
            String remarks) {
        Loan loan = findLoan(loanId);

        if (loan.getLoanStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException(
                    "Level-1 approval only allowed in PENDING state");
        }
        if (loan.getLevel1ApprovedBy() != null) {
            throw new IllegalStateException("Level-1 already approved");
        }

        loan.setLevel1ApprovedBy(approvedBy);
        loan.setLevel1ApprovedAt(LocalDateTime.now());
        if (remarks != null) {
            loan.setRemarks(remarks);
        }

        return toDto(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDto approveLevel2(
            Long loanId,
            String approvedBy,
            String remarks) {
        Loan loan = findLoan(loanId);

        if (loan.getLevel1ApprovedBy() == null) {
            throw new IllegalStateException(
                    "Level-1 approval required before Level-2");
        }

        stateMachine.validateTransition(
                loan.getLoanStatus(),
                LoanStatus.APPROVED);

        loan.setLevel2ApprovedBy(approvedBy);
        loan.setLevel2ApprovedAt(LocalDateTime.now());
        loan.setLoanStatus(LoanStatus.APPROVED);
        loan.setEmi(
                emiCalculator.calculate(
                        loan.getLoanAmount(),
                        loan.getInterestRate(),
                        loan.getTenureMonths()));

        if (remarks != null) {
            loan.setRemarks(remarks);
        }

        loan = loanRepository.save(loan);

        LoanEventPayload payload = LoanEventPayload.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .status(LoanStatus.APPROVED.name())
                .previousStatus(LoanStatus.PENDING.name())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .emi(loan.getEmi())
                .tenureMonths(loan.getTenureMonths())
                .approvedBy(approvedBy)
                .build();

        outboxService.enqueue(
                DomainEvent.of(
                        LoanEvents.LOAN_APPROVED,
                        loan.getLoanId().toString(),
                        "Loan",
                        payload));

        return toDto(loan);
    }

    @Transactional
    public LoanResponseDto reject(
            Long loanId,
            String rejectedBy,
            String remarks) {
        Loan loan = findLoan(loanId);
        stateMachine.validateTransition(
                loan.getLoanStatus(),
                LoanStatus.REJECTED);

        String previous = loan.getLoanStatus().name();
        loan.setLoanStatus(LoanStatus.REJECTED);
        loan.setRemarks(
                remarks != null
                        ? remarks
                        : "Rejected by " + rejectedBy);
        loan = loanRepository.save(loan);

        LoanEventPayload payload = LoanEventPayload.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .status(LoanStatus.REJECTED.name())
                .previousStatus(previous)
                .remarks(loan.getRemarks())
                .build();

        outboxService.enqueue(
                DomainEvent.of(
                        LoanEvents.LOAN_REJECTED,
                        loan.getLoanId().toString(),
                        "Loan",
                        payload));

        return toDto(loan);
    }

    private void validateCustomer(Long customerId) {
        customerValidationService.validate(customerId);
    }

    private Loan findLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id));
    }

    private LoanResponseDto toDto(Loan loan) {
        return LoanResponseDto.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomerId())
                .productId(loan.getProductId())
                .loanType(loan.getLoanType())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .emi(loan.getEmi())
                .loanStatus(loan.getLoanStatus())
                .applicationDate(loan.getApplicationDate())
                .remarks(loan.getRemarks())
                .outstandingPrincipal(loan.getOutstandingPrincipal())
                .paidInstallments(loan.getPaidInstallments())
                .remainingInstallments(loan.getRemainingInstallments())
                .disbursementDate(loan.getDisbursementDate())
                .nextDueDate(loan.getNextDueDate())
                .level1ApprovedBy(loan.getLevel1ApprovedBy())
                .level1ApprovedAt(loan.getLevel1ApprovedAt())
                .level2ApprovedBy(loan.getLevel2ApprovedBy())
                .level2ApprovedAt(loan.getLevel2ApprovedAt())
                .build();
    }
}
