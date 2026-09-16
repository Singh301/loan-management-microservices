package com.loanmanagement.repayment.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.repayment.dto.RepaymentRequestDto;
import com.loanmanagement.repayment.entity.EmiSchedule;
import com.loanmanagement.repayment.entity.Repayment;
import com.loanmanagement.repayment.repository.EmiScheduleRepository;
import com.loanmanagement.repayment.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;

    @Transactional
    public Repayment recordPayment(RepaymentRequestDto request) {
        LocalDate payDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        Repayment repayment = Repayment.builder()
                .loanId(request.getLoanId())
                .emiScheduleId(request.getEmiScheduleId())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .transactionRef(request.getTransactionRef())
                .paymentDate(payDate)
                .remarks(request.getRemarks())
                .build();

        repayment = repaymentRepository.save(repayment);

        // Update EMI schedule if linked
        if (request.getEmiScheduleId() != null) {
            EmiSchedule schedule = emiScheduleRepository.findById(request.getEmiScheduleId())
                    .orElseThrow(() -> new ResourceNotFoundException("EmiSchedule", request.getEmiScheduleId()));

            if (schedule.getStatus() == EmiSchedule.Status.PAID) {
                throw new DomainException("EMI already paid", HttpStatus.CONFLICT);
            }

            BigDecimal totalDue = schedule.getEmiAmount().add(schedule.getLateFee());
            schedule.setPaidAmount(request.getAmount());
            schedule.setPaidDate(payDate);

            if (request.getAmount().compareTo(totalDue) >= 0) {
                schedule.setStatus(EmiSchedule.Status.PAID);
            } else {
                schedule.setStatus(EmiSchedule.Status.PARTIALLY_PAID);
            }
            emiScheduleRepository.save(schedule);
        }

        log.info("Repayment recorded: loan={}, amount={}", request.getLoanId(), request.getAmount());
        return repayment;
    }

    @Transactional(readOnly = true)
    public List<Repayment> getByLoan(Long loanId) {
        return repaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
    }

    @Transactional
    public BigDecimal calculateForeclosureAmount(Long loanId) {
        List<EmiSchedule> pending = emiScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .filter(s -> s.getStatus() != EmiSchedule.Status.PAID)
                .toList();

        return pending.stream()
                .map(s -> s.getPrincipalComponent().add(s.getLateFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
