package com.loanmanagement.loan.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.loan.dto.CollateralDto;
import com.loanmanagement.loan.service.CollateralService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collaterals")
@RequiredArgsConstructor
@Tag(name = "Collaterals")
public class CollateralController {

    private final CollateralService collateralService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CollateralDto.Response>> add(@Valid @RequestBody CollateralDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Collateral added", collateralService.add(dto)));
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CollateralDto.Response>>> byLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(collateralService.byLoan(loanId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        collateralService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
