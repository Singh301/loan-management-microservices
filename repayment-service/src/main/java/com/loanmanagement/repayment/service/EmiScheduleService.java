package com.loanmanagement.repayment.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.repayment.entity.EmiSchedule;
import com.loanmanagement.repayment.repository.EmiScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmiScheduleService {

    private final EmiScheduleRepository emiScheduleRepository;

    @Transactional
    public List<EmiSchedule> generateSchedule(Long loanId, BigDecimal principal, BigDecimal annualRate,
                                               int tenureMonths, BigDecimal emi, LocalDate startDate) {
        if (emiScheduleRepository.existsByLoanId(loanId)) {
            throw new DomainException("EMI schedule already exists for loan " + loanId, HttpStatus.CONFLICT);
        }

        List<EmiSchedule> schedules = new ArrayList<>();
        BigDecimal remainingPrincipal = principal;
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), new MathContext(10, RoundingMode.HALF_UP));

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalComp = emi.subtract(interest).setScale(2, RoundingMode.HALF_UP);

            // Last installment adjustment
            if (i == tenureMonths) {
                principalComp = remainingPrincipal;
                interest = emi.subtract(principalComp).max(BigDecimal.ZERO);
            }

            EmiSchedule schedule = EmiSchedule.builder()
                    .loanId(loanId)
                    .installmentNumber(i)
                    .dueDate(startDate.plusMonths(i))
                    .principalComponent(principalComp)
                    .interestComponent(interest)
                    .emiAmount(principalComp.add(interest))
                    .status(EmiSchedule.Status.PENDING)
                    .build();

            schedules.add(schedule);
            remainingPrincipal = remainingPrincipal.subtract(principalComp);
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                remainingPrincipal = BigDecimal.ZERO;
            }
        }

        return emiScheduleRepository.saveAll(schedules);
    }

    @Transactional(readOnly = true)
    public List<EmiSchedule> getByLoanId(Long loanId) {
        return emiScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
    }

    @Transactional
    public void markOverdue() {
        LocalDate today = LocalDate.now();
        List<EmiSchedule> overdue = emiScheduleRepository.findByStatusAndDueDateBefore(
                EmiSchedule.Status.PENDING, today);
        for (EmiSchedule s : overdue) {
            s.setStatus(EmiSchedule.Status.OVERDUE);
            // Simple late fee: 2% of EMI
            s.setLateFee(s.getEmiAmount().multiply(BigDecimal.valueOf(0.02)).setScale(2, RoundingMode.HALF_UP));
        }
        emiScheduleRepository.saveAll(overdue);
        log.info("Marked {} EMI schedules as OVERDUE", overdue.size());
    }
}
