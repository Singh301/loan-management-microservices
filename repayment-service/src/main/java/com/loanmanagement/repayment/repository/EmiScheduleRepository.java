package com.loanmanagement.repayment.repository;

import com.loanmanagement.repayment.entity.EmiSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {
    List<EmiSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmiSchedule e where e.id = :id")
    java.util.Optional<EmiSchedule> findByIdForUpdate(Long id);

    List<EmiSchedule> findByStatusAndDueDateBefore(EmiSchedule.Status status, LocalDate date);
    boolean existsByLoanId(Long loanId);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO emi_schedules
                (loan_id, installment_number, due_date, principal_component, interest_component,
                 emi_amount, late_fee, status, paid_date, paid_amount, created_at)
            VALUES
                (:loanId, :installmentNumber, :dueDate, :principalComponent, :interestComponent,
                 :emiAmount, 0, :status, NULL, NULL, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("loanId") Long loanId,
            @Param("installmentNumber") Integer installmentNumber,
            @Param("dueDate") LocalDate dueDate,
            @Param("principalComponent") BigDecimal principalComponent,
            @Param("interestComponent") BigDecimal interestComponent,
            @Param("emiAmount") BigDecimal emiAmount,
            @Param("status") String status);
}
