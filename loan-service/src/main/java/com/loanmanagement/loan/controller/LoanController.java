package com.loanmanagement.loan.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.dto.PageResponse;
import com.loanmanagement.loan.dto.LoanRequestDto;
import com.loanmanagement.loan.dto.LoanResponseDto;
import com.loanmanagement.loan.entity.LoanStatus;
import com.loanmanagement.loan.entity.LoanType;
import com.loanmanagement.loan.service.LoanApplicationService;
import com.loanmanagement.loan.service.LoanDisbursementService;
import com.loanmanagement.loan.service.LoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Loan application, approval, disbursement, search, analytics")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanApplicationService applicationService;
    private final LoanDisbursementService disbursementService;
    private final LoanQueryService queryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'MANAGER')")
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<ApiResponse<LoanResponseDto>> apply(@Valid @RequestBody LoanRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan application submitted", applicationService.apply(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all loans (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> listAll(
            @RequestParam(required = false) LoanType loanType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "loanId") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return page(queryService.listAll(loanType, page, size, sortBy, direction));
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> getById(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getById(loanId)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> getByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LoanResponseDto> result = applicationService.getByCustomer(customerId, PageRequest.of(page, size));
        return page(result);
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current customer's loans (self-service)")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> myLoans(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long customerId = extractCustomerId(auth);
        Page<LoanResponseDto> result = applicationService.getByCustomer(customerId, PageRequest.of(page, size));
        return page(result);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> byStatus(
            @PathVariable LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(queryService.byStatus(status, page, size));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Search loans by type/status/amount range")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> search(
            @RequestParam(required = false) LoanType loanType,
            @RequestParam(required = false) LoanStatus loanStatus,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return page(queryService.search(loanType, loanStatus, minAmount, maxAmount, page, size));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Loan portfolio statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics() {
        return ResponseEntity.ok(ApiResponse.success(queryService.statistics()));
    }

    @GetMapping("/reports/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Monthly loan application report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> monthlyReport(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(queryService.monthlyReport(year, month)));
    }

    @GetMapping("/{loanId}/statement")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Loan statement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statement(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.statement(loanId)));
    }

    @GetMapping("/{loanId}/foreclosure")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Foreclosure amount details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> foreclosure(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.foreclosureDetails(loanId)));
    }

    @PutMapping("/{loanId}/approve/level1")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Manager Level-1 approval")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approveLevel1(
            @PathVariable Long loanId, @RequestParam(required = false) String remarks, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Level-1 approved",
                applicationService.approveLevel1(loanId, auth.getName(), remarks)));
    }

    @PutMapping("/{loanId}/approve/level2")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin Level-2 approval → APPROVED + EMI")
    public ResponseEntity<ApiResponse<LoanResponseDto>> approveLevel2(
            @PathVariable Long loanId, @RequestParam(required = false) String remarks, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Loan approved",
                applicationService.approveLevel2(loanId, auth.getName(), remarks)));
    }

    @PutMapping("/{loanId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponseDto>> reject(
            @PathVariable Long loanId, @RequestParam(required = false) String remarks, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Loan rejected",
                applicationService.reject(loanId, auth.getName(), remarks)));
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disburse loan (idempotent via Idempotency-Key)")
    public ResponseEntity<ApiResponse<LoanResponseDto>> disburse(
            @PathVariable Long loanId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed",
                disbursementService.disburse(loanId, idempotencyKey, auth.getName())));
    }

    @PostMapping("/{loanId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Close loan after full repayment / foreclosure")
    public ResponseEntity<ApiResponse<LoanResponseDto>> close(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success("Loan closed", queryService.closeLoan(loanId)));
    }

    private ResponseEntity<ApiResponse<PageResponse<LoanResponseDto>>> page(Page<LoanResponseDto> result) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.<LoanResponseDto>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build()));
    }

    /**
     * Extract customerId from JWT principal.
     * JwtAuthenticationFilter sets principal name as userId or username; for CUSTOMER role
     * the claim "customerId" or userId is used. Adjust according to your JWT claims.
     */
    private Long extractCustomerId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            try {
                return Long.parseLong(ud.getUsername());
            } catch (NumberFormatException e) {
                // username is not numeric – in real system map username → customerId via auth-service
                throw new IllegalStateException("Unable to resolve customerId from token. Ensure JWT contains numeric user/customer id.");
            }
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Unable to resolve customerId from token");
        }
    }
}
