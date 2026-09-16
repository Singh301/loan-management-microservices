package com.loanmanagement.loan.dto;

import com.loanmanagement.loan.entity.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRequestDto {
    @NotNull
    private Long customerId;

    private Long productId;

    @NotNull
    private LoanType loanType;

    @NotNull
    @DecimalMin("1000.00")
    private BigDecimal loanAmount;

    @NotNull
    @DecimalMin("1.00")
    @DecimalMax("50.00")
    private BigDecimal interestRate;

    @NotNull
    @Min(1)
    @Max(360)
    private Integer tenureMonths;

    private String remarks;
}
