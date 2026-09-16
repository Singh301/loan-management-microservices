package com.loanmanagement.loan.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.loan.dto.LoanProductDto;
import com.loanmanagement.loan.entity.LoanType;
import com.loanmanagement.loan.service.LoanProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loan-products")
@RequiredArgsConstructor
@Tag(name = "Loan Products")
public class LoanProductController {

    private final LoanProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanProductDto.Response>> create(@Valid @RequestBody LoanProductDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.create(dto)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<ApiResponse<List<LoanProductDto.Response>>> list() {
        return ResponseEntity.ok(ApiResponse.success(productService.listActive()));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<ApiResponse<List<LoanProductDto.Response>>> byType(@PathVariable LoanType type) {
        return ResponseEntity.ok(ApiResponse.success(productService.byType(type)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<ApiResponse<LoanProductDto.Response>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.get(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoanProductDto.Response>> update(
            @PathVariable Long id, @Valid @RequestBody LoanProductDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Updated", productService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        productService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Deactivated", null));
    }
}
