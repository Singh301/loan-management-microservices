package com.loanmanagement.repayment.repository;

import com.loanmanagement.repayment.entity.EmiSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
