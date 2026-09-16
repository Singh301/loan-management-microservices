package com.loanmanagement.loan.repository;

import com.loanmanagement.loan.entity.LoanApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanApprovalRepository extends JpaRepository<LoanApproval, Long> {
    List<LoanApproval> findByLoanIdOrderByLevelAsc(Long loanId);
}
