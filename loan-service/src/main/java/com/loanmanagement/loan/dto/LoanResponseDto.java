package com.loanmanagement.loan.dto;

import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.entity.LoanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponseDto {
    private Long loanId;
    private Long customerId;
    private Long productId;
    private LoanType loanType;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emi;
    private LoanStatus loanStatus;
    private LocalDate applicationDate;
    private String remarks;
    private BigDecimal outstandingPrincipal;
    private Integer paidInstallments;
    private Integer remainingInstallments;
    private LocalDate disbursementDate;
    private LocalDate nextDueDate;
    private String level1ApprovedBy;
    private LocalDateTime level1ApprovedAt;
    private String level2ApprovedBy;
    private LocalDateTime level2ApprovedAt;
}
