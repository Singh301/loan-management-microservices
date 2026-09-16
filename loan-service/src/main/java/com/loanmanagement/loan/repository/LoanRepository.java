package com.loanmanagement.loan.repository;

import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.entity.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Page<Loan> findByCustomerId(Long customerId, Pageable pageable);
    List<Loan> findByCustomerId(Long customerId);
    Page<Loan> findByLoanStatus(LoanStatus status, Pageable pageable);
    Page<Loan> findByLoanType(LoanType type, Pageable pageable);
    Optional<Loan> findByDisbursementIdempotencyKey(String key);
    boolean existsByDisbursementIdempotencyKey(String key);

    long countByLoanStatus(LoanStatus status);

    @Query("SELECT COALESCE(SUM(l.loanAmount), 0) FROM Loan l WHERE l.loanStatus = :status")
    BigDecimal sumAmountByStatus(@Param("status") LoanStatus status);

    @Query("SELECT COALESCE(SUM(l.loanAmount), 0) FROM Loan l")
    BigDecimal sumAllAmounts();

    @Query("SELECT COALESCE(AVG(l.loanAmount), 0) FROM Loan l")
    BigDecimal avgAmount();

    @Query("SELECT l FROM Loan l WHERE " +
            "(:loanType IS NULL OR l.loanType = :loanType) AND " +
            "(:loanStatus IS NULL OR l.loanStatus = :loanStatus) AND " +
            "(:minAmount IS NULL OR l.loanAmount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR l.loanAmount <= :maxAmount)")
    Page<Loan> search(
            @Param("loanType") LoanType loanType,
            @Param("loanStatus") LoanStatus loanStatus,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);

    @Query("SELECT COUNT(l), COALESCE(SUM(l.loanAmount), 0), COALESCE(AVG(l.loanAmount), 0) " +
            "FROM Loan l WHERE l.applicationDate >= :startDate AND l.applicationDate < :endDate")
    Object[] monthlyAggregate(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);
}
