package com.loanmanagement.audit.controller;

import com.loanmanagement.audit.entity.AuditLog;
import com.loanmanagement.audit.service.AuditService;
import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> result = auditService.findAll(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.<AuditLog>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build()));
    }

    @GetMapping("/aggregate/{aggregateId}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> byAggregate(
            @PathVariable String aggregateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AuditLog> result = auditService.findByAggregate(aggregateId, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.<AuditLog>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build()));
    }
}
