package com.loanmanagement.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEventPayload {
    private Long loanId;
    private Long customerId;
    private String customerEmail;
    private String status;
    private String previousStatus;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private BigDecimal emi;
    private Integer tenureMonths;
    private LocalDate applicationDate;
    private LocalDate disbursementDate;
    private String remarks;
    private String approvedBy;
}
