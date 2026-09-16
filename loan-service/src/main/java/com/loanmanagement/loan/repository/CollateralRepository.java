package com.loanmanagement.loan.repository;

import com.loanmanagement.loan.entity.Collateral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollateralRepository extends JpaRepository<Collateral, Long> {
    List<Collateral> findByLoanId(Long loanId);
}
