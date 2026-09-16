package com.loanmanagement.loan.repository;

import com.loanmanagement.loan.entity.LoanProduct;
import com.loanmanagement.loan.entity.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    Optional<LoanProduct> findByProductCode(String productCode);
    List<LoanProduct> findByActiveTrue();
    List<LoanProduct> findByLoanTypeAndActiveTrue(LoanType loanType);
    boolean existsByProductCode(String productCode);
}
