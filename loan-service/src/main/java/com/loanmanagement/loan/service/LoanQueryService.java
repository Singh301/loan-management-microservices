package com.loanmanagement.loan.service;

import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.entity.Loan;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.entity.LoanType;
import com.loanmanagement.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final LoanApplicationService applicationService;

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> listAll(
            LoanType type,
            int page,
            int size,
            String sortBy,
            String direction) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        PageRequest pr = PageRequest.of(page, size, sort);
        Page<Loan> result = type != null
                ? loanRepository.findByLoanType(type, pr)
                : loanRepository.findAll(pr);
        return result.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> byStatus(LoanStatus status, int page, int size) {
        return loanRepository.findByLoanStatus(
                status,
                PageRequest.of(page, size)).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponseDto> search(
            LoanType type,
            LoanStatus status,
            BigDecimal min,
            BigDecimal max,
            int page,
            int size) {
        return loanRepository.search(
                type,
                status,
                min,
                max,
                PageRequest.of(page, size)).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLoans", loanRepository.count());
        stats.put("pendingLoans", loanRepository.countByLoanStatus(LoanStatus.PENDING));
        stats.put("approvedLoans", loanRepository.countByLoanStatus(LoanStatus.APPROVED));
        stats.put("rejectedLoans", loanRepository.countByLoanStatus(LoanStatus.REJECTED));
        stats.put("activeLoans", loanRepository.countByLoanStatus(LoanStatus.ACTIVE));
        stats.put("overdueLoans", loanRepository.countByLoanStatus(LoanStatus.OVERDUE));
        stats.put("closedLoans", loanRepository.countByLoanStatus(LoanStatus.CLOSED));
        stats.put("totalLoanAmount", loanRepository.sumAllAmounts());
        stats.put(
                "approvedLoanAmount",
                loanRepository.sumAmountByStatus(LoanStatus.APPROVED)
                        .add(loanRepository.sumAmountByStatus(LoanStatus.ACTIVE))
                        .add(loanRepository.sumAmountByStatus(LoanStatus.DISBURSED)));
        stats.put("averageLoanAmount", loanRepository.avgAmount());
        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> monthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        Object[] aggregate = loanRepository.monthlyAggregate(start, end);

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("loanCount", aggregate[0]);
        report.put("totalLoanAmount", aggregate[1]);
        report.put("averageLoanAmount", aggregate[2]);
        report.put("from", start);
        report.put("toExclusive", end);
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> foreclosureDetails(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        BigDecimal outstanding = loan.getOutstandingPrincipal() != null
                ? loan.getOutstandingPrincipal()
                : loan.getLoanAmount();
        BigDecimal lateFee = loan.getTotalLateFee() != null
                ? loan.getTotalLateFee()
                : BigDecimal.ZERO;
        BigDecimal charge = outstanding
                .multiply(BigDecimal.valueOf(0.02))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = outstanding.add(lateFee).add(charge);

        Map<String, Object> result = new HashMap<>();
        result.put("loanId", loanId);
        result.put("loanStatus", loan.getLoanStatus());
        result.put("outstandingPrincipal", outstanding);
        result.put("totalLateFee", lateFee);
        result.put("foreclosureCharge", charge);
        result.put("totalPayable", total);
        result.put("emi", loan.getEmi());
        result.put("paidInstallments", loan.getPaidInstallments());
        result.put("remainingInstallments", loan.getRemainingInstallments());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> statement(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        Map<String, Object> stmt = new HashMap<>();
        stmt.put("loanId", loan.getLoanId());
        stmt.put("customerId", loan.getCustomerId());
        stmt.put("loanType", loan.getLoanType());
        stmt.put("loanStatus", loan.getLoanStatus());
        stmt.put("loanAmount", loan.getLoanAmount());
        stmt.put("interestRate", loan.getInterestRate());
        stmt.put("tenureMonths", loan.getTenureMonths());
        stmt.put("emi", loan.getEmi());
        stmt.put("outstandingPrincipal", loan.getOutstandingPrincipal());
        stmt.put("paidInstallments", loan.getPaidInstallments());
        stmt.put("remainingInstallments", loan.getRemainingInstallments());
        stmt.put("disbursementDate", loan.getDisbursementDate());
        stmt.put("nextDueDate", loan.getNextDueDate());
        stmt.put("totalLateFee", loan.getTotalLateFee());
        stmt.put("applicationDate", loan.getApplicationDate());
        return stmt;
    }

    @Transactional
    public LoanResponseDto closeLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        loan.setLoanStatus(LoanStatus.CLOSED);
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setRemainingInstallments(0);
        loanRepository.save(loan);
        return toDto(loan);
    }

    private LoanResponseDto toDto(Loan loan) {
        return applicationService.getById(loan.getLoanId());
    }
}
