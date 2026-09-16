package com.loanmanagement.repayment.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.repayment.entity.EmiSchedule;
import com.loanmanagement.repayment.service.EmiScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/emi-schedules")
@RequiredArgsConstructor
@Tag(name = "EMI Schedules")
public class EmiScheduleController {

    private final EmiScheduleService emiScheduleService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Generate EMI schedule for a disbursed loan")
    public ResponseEntity<ApiResponse<List<EmiSchedule>>> generate(
            @RequestBody Map<String, Object> body) {
        Long loanId = Long.valueOf(body.get("loanId").toString());
        BigDecimal principal = new BigDecimal(body.get("principal").toString());
        BigDecimal rate = new BigDecimal(body.get("interestRate").toString());
        int tenure = Integer.parseInt(body.get("tenureMonths").toString());
        BigDecimal emi = new BigDecimal(body.get("emi").toString());
        LocalDate start = body.containsKey("startDate")
                ? LocalDate.parse(body.get("startDate").toString())
                : LocalDate.now();

        List<EmiSchedule> schedules = emiScheduleService.generateSchedule(
                loanId, principal, rate, tenure, emi, start);
        return ResponseEntity.ok(ApiResponse.success("EMI schedule generated", schedules));
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<EmiSchedule>>> getByLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(emiScheduleService.getByLoanId(loanId)));
    }
}
