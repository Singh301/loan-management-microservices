package com.loanmanagement.loan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("deleted = false")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id")
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(precision = 15, scale = 2)
    private BigDecimal emi;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_status", nullable = false)
    @Builder.Default
    private LoanStatus loanStatus = LoanStatus.PENDING;

    @Column(name = "application_date", nullable = false)
    @Builder.Default
    private LocalDate applicationDate = LocalDate.now();

    @Column(length = 500)
    private String remarks;

    @Column(name = "outstanding_principal", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(name = "paid_installments")
    @Builder.Default
    private Integer paidInstallments = 0;

    @Column(name = "remaining_installments")
    @Builder.Default
    private Integer remainingInstallments = 0;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "total_late_fee", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLateFee = BigDecimal.ZERO;

    @Column(name = "disbursement_idempotency_key", length = 100)
    private String disbursementIdempotencyKey;

    @Column(name = "level1_approved_by")
    private String level1ApprovedBy;

    @Column(name = "level1_approved_at")
    private LocalDateTime level1ApprovedAt;

    @Column(name = "level2_approved_by")
    private String level2ApprovedBy;

    @Column(name = "level2_approved_at")
    private LocalDateTime level2ApprovedAt;

    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
