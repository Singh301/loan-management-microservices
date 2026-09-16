package com.loanmanagement.repayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentRequestDto {
    @NotNull
    private Long loanId;

    private Long emiScheduleId;

    @NotNull
    @DecimalMin("1.00")
    private BigDecimal amount;

    @NotBlank
    private String paymentMode; // UPI, NEFT, CASH, CARD

    private String transactionRef;
    private LocalDate paymentDate;
    private String remarks;
}
