package com.loanmanagement.customer.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.dto.PageResponse;
import com.loanmanagement.customer.dto.CustomerRequestDto;
import com.loanmanagement.customer.dto.CustomerResponseDto;
import com.loanmanagement.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    @Operation(summary = "Create customer")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> create(
            @Valid @RequestBody CustomerRequestDto request, Authentication authentication) {
        if (hasRole(authentication, "CUSTOMER")) {
            request.setUserId(currentUserId(authentication));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created", customerService.create(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                hasRole(authentication, "CUSTOMER")
                        ? customerService.getByIdForUser(id, currentUserId(authentication))
                        : customerService.getById(id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getByUserId(currentUserId(authentication))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CustomerResponseDto> result = customerService.getAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.<CustomerResponseDto>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Updated",
                hasRole(authentication, "CUSTOMER")
                        ? customerService.updateForUser(id, request, currentUserId(authentication))
                        : customerService.update(id, request)));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof Number number)) {
            throw new IllegalStateException("Unable to resolve userId from token");
        }
        return number.longValue();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        customerService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
