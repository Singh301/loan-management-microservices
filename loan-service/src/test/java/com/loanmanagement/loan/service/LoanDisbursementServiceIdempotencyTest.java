package com.loanmanagement.loan.service;

import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.loan.domain.LoanStateMachine;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.idempotency.IdempotencyService;
import com.loanmanagement.loan.outbox.OutboxService;
import com.loanmanagement.loan.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDisbursementServiceIdempotencyTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanStateMachine stateMachine;
    @Mock
    private OutboxService outboxService;
    @Mock
    private LoanApplicationService applicationService;
    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private LoanDisbursementService service;

    @Test
    void shouldReturnCompletedIdempotentResponseWithoutDisbursingAgain() {
        Loan loan = approvedLoan(1L);
        LoanResponseDto existing = LoanResponseDto.builder()
                .loanId(1L)
                .customerId(10L)
                .loanAmount(new BigDecimal("500000.00"))
                .loanStatus(LoanStatus.ACTIVE)
                .build();

        when(loanRepository.findByDisbursementIdempotencyKey("DISB-123")).thenReturn(Optional.empty());
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(idempotencyService.requestHash("LOAN_DISBURSE:1")).thenReturn("hash");
        when(idempotencyService.findExisting(
                "DISB-123", "hash", IdempotencyService.DISBURSE_OPERATION))
                .thenReturn(existing);

        LoanResponseDto result = service.disburse(1L, "DISB-123", "admin");

        assertSame(existing, result);
        verify(idempotencyService, never()).claim(anyString(), anyString(), anyString());
        verify(loanRepository, never()).save(any(Loan.class));
        verify(outboxService, never()).enqueue(any());
    }

    @Test
    void shouldClaimAndCompleteNewDisbursementKey() {
        Loan loan = approvedLoan(2L);
        LoanResponseDto response = LoanResponseDto.builder()
                .loanId(2L)
                .customerId(10L)
                .loanAmount(new BigDecimal("500000.00"))
                .loanStatus(LoanStatus.ACTIVE)
                .build();

        when(loanRepository.findByDisbursementIdempotencyKey("DISB-456")).thenReturn(Optional.empty());
        when(loanRepository.findById(2L)).thenReturn(Optional.of(loan));
        when(idempotencyService.requestHash("LOAN_DISBURSE:2")).thenReturn("hash-2");
        when(idempotencyService.findExisting(
                "DISB-456", "hash-2", IdempotencyService.DISBURSE_OPERATION))
                .thenReturn(null);
        when(loanRepository.save(loan)).thenReturn(loan);
        when(applicationService.getById(2L)).thenReturn(response);

        LoanResponseDto result = service.disburse(2L, "DISB-456", "admin");

        assertSame(response, result);
        verify(idempotencyService).claim(
                "DISB-456", "hash-2", IdempotencyService.DISBURSE_OPERATION);
        verify(idempotencyService).complete(
                "DISB-456", response, IdempotencyService.DISBURSE_OPERATION);
        verify(loanRepository).save(loan);
        verify(outboxService).enqueue(any());
    }

    private Loan approvedLoan(Long loanId) {
        return Loan.builder()
                .loanId(loanId)
                .customerId(10L)
                .loanType(com.loanmanagement.loan.entity.LoanType.PERSONAL)
                .loanAmount(new BigDecimal("500000.00"))
                .interestRate(new BigDecimal("10.00"))
                .tenureMonths(24)
                .emi(new BigDecimal("23072.15"))
                .loanStatus(LoanStatus.APPROVED)
                .build();
    }
}
