package com.loanmanagement.document.controller;

import com.loanmanagement.common.dto.ApiResponse;
import com.loanmanagement.document.entity.Document;
import com.loanmanagement.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Upload document (PDF/JPEG/PNG/WEBP, max 5MB)")
    public ResponseEntity<ApiResponse<Document>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long loanId,
            @RequestParam(required = false) Long customerId,
            @RequestParam String documentType,
            Authentication authentication) {
        Document doc = documentService.upload(file, loanId, customerId, documentType,
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Uploaded", doc));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Document meta = documentService.getMeta(id);
        Resource resource = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(meta.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalName().replace("\"", "") + "\"")
                .body(resource);
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Document>>> byLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.byLoan(loanId)));
    }

    @PutMapping("/{id}/verification")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Verify or reject a document")
    public ResponseEntity<ApiResponse<Document>> verify(
            @PathVariable Long id,
            @RequestParam Document.VerificationStatus status,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Document verification updated",
                documentService.verify(id, status, remarks, authentication.getName())));
    }
}
