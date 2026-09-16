package com.loanmanagement.loan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_approvals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "approved_by", nullable = false)
    private String approvedBy;

    @Column(nullable = false, length = 20)
    private String decision; // APPROVED / REJECTED

    private String remarks;

    @Column(name = "decided_at")
    @Builder.Default
    private LocalDateTime decidedAt = LocalDateTime.now();
}
