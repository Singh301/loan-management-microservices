package com.loanmanagement.repayment.service;

import com.loanmanagement.repayment.dto.RepaymentRequestDto;
import com.loanmanagement.repayment.entity.EmiSchedule;
import com.loanmanagement.repayment.repository.EmiScheduleRepository;
import com.loanmanagement.repayment.repository.RepaymentRepository;
import com.loanmanagement.repayment.entity.Repayment;
import com.loanmanagement.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTransactionRefTest {

    @Mock
    private RepaymentRepository repaymentRepository;

    @Mock
    private EmiScheduleRepository emiScheduleRepository;

    @InjectMocks
    private RepaymentService repaymentService;

    @Test
    void shouldRejectDuplicateTransactionReferenceAtomically() {
        RepaymentRequestDto request = new RepaymentRequestDto();
        request.setLoanId(100L);
        request.setEmiScheduleId(200L);
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMode("UPI");
        request.setTransactionRef("TXN-123");
        request.setPaymentDate(LocalDate.now());

        EmiSchedule schedule = EmiSchedule.builder()
                .id(200L)
                .loanId(100L)
                .emiAmount(new BigDecimal("5000.00"))
                .lateFee(BigDecimal.ZERO)
                .status(EmiSchedule.Status.PENDING)
                .build();

        when(emiScheduleRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(schedule));
        when(repaymentRepository.insertIfAbsent(
                eq(100L), eq(200L), eq(new BigDecimal("5000.00")),
                eq("UPI"), eq("TXN-123"), any(LocalDate.class), isNull()))
                .thenReturn(0);

        DomainException ex = assertThrows(DomainException.class,
                () -> repaymentService.recordPayment(request));

        assertEquals("PAYMENT_ALREADY_PROCESSED", ex.getErrorCode());
    }
}
