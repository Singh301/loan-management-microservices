package com.loanmanagement.repayment.repository;

import com.loanmanagement.repayment.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);
    boolean existsByTransactionRef(String transactionRef);
}
