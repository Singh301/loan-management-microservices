package com.loanmanagement.repayment.repository;

import com.loanmanagement.repayment.entity.EmiSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {
    List<EmiSchedule> findByLoanIdOrderByInstallmentNumberAsc(Long loanId);
    List<EmiSchedule> findByStatusAndDueDateBefore(EmiSchedule.Status status, LocalDate date);
    boolean existsByLoanId(Long loanId);
}
