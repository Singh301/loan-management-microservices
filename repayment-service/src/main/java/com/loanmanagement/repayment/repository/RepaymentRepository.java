package com.loanmanagement.repayment.repository;

import com.loanmanagement.repayment.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
    List<Repayment> findByLoanIdOrderByPaymentDateDesc(Long loanId);
    java.util.Optional<Repayment> findByTransactionRef(String transactionRef);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO repayments
                (loan_id, emi_schedule_id, amount, payment_mode, transaction_ref, payment_date, remarks, created_at)
            VALUES
                (:loanId, :emiScheduleId, :amount, :paymentMode, :transactionRef, :paymentDate, :remarks, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("loanId") Long loanId,
            @Param("emiScheduleId") Long emiScheduleId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("paymentMode") String paymentMode,
            @Param("transactionRef") String transactionRef,
            @Param("paymentDate") java.time.LocalDate paymentDate,
            @Param("remarks") String remarks);
}
