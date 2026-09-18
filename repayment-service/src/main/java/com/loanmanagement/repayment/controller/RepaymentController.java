package com.loanmanagement.repayment.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.repayment.dto.RepaymentRequestDto;
import com.loanmanagement.repayment.entity.Repayment;
import com.loanmanagement.repayment.service.LoanOwnershipService;
import com.loanmanagement.repayment.service.RepaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/repayments")
@RequiredArgsConstructor
@Tag(name = "Repayments")
public class RepaymentController {

    private final RepaymentService repaymentService;
    private final LoanOwnershipService loanOwnershipService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Record a repayment / pay EMI")
    public ResponseEntity<ApiResponse<Repayment>> pay(
            @Valid @RequestBody RepaymentRequestDto request, Authentication authentication) {
        loanOwnershipService.validateCustomerAccess(request.getLoanId(), authentication);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", repaymentService.recordPayment(request)));
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Repayment>>> byLoan(
            @PathVariable Long loanId, Authentication authentication) {
        loanOwnershipService.validateCustomerAccess(loanId, authentication);
        return ResponseEntity.ok(ApiResponse.success(repaymentService.getByLoan(loanId)));
    }

    @GetMapping("/loan/{loanId}/foreclosure-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Calculate foreclosure amount (remaining principal + late fees)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> foreclosureAmount(
            @PathVariable Long loanId, Authentication authentication) {
        loanOwnershipService.validateCustomerAccess(loanId, authentication);
        BigDecimal amount = repaymentService.calculateForeclosureAmount(loanId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "loanId", loanId,
                "foreclosureAmount", amount
        )));
    }
}
