package com.loanmanagement.repayment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100)
    private String transactionRef;
    private LocalDate paymentDate;
    private String remarks;
}
