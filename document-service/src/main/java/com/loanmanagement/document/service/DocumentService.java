package com.loanmanagement.document.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.document.entity.Document;
import com.loanmanagement.document.repository.DocumentRepository;
import com.loanmanagement.document.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final DocumentStorage documentStorage;

    @Transactional
    public Document upload(MultipartFile file, Long loanId, Long customerId,
                           String documentType, String uploadedBy) {
        validate(file);

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeOriginal = Paths.get(original.replace("\\", "/")).getFileName().toString();

        String storageKey = null;
        try {
            storageKey = documentStorage.store(file, safeOriginal);

            Document doc = Document.builder()
                    .loanId(loanId)
                    .customerId(customerId)
                    .documentType(documentType)
                    .originalName(safeOriginal)
                    .storedName(storageKey)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storagePath(storageKey)
                    .uploadedBy(uploadedBy)
                    .build();

            return documentRepository.save(doc);
        } catch (IOException ex) {
            cleanupQuietly(storageKey);
            throw new DomainException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException ex) {
            cleanupQuietly(storageKey);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) {
        Document doc = getMeta(id);
        try {
            return documentStorage.load(doc.getStoragePath());
        } catch (IOException ex) {
            throw new DomainException("File not readable", HttpStatus.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public Document getMeta(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    @Transactional(readOnly = true)
    public List<Document> byLoan(Long loanId) {
        return documentRepository.findByLoanId(loanId);
    }

    @Transactional
    public Document verify(Long id, Document.VerificationStatus status, String remarks, String verifiedBy) {
        if (status == Document.VerificationStatus.PENDING) {
            throw new DomainException(
                    "Verification status must be VERIFIED or REJECTED", HttpStatus.BAD_REQUEST);
        }
        Document document = getMeta(id);
        document.setVerificationStatus(status);
        document.setVerificationRemarks(remarks);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    private void cleanupQuietly(String storageKey) {
        if (storageKey == null) {
            return;
        }
        try {
            documentStorage.delete(storageKey);
        } catch (IOException cleanupError) {
            log.error("Document storage cleanup failed for key={}", storageKey, cleanupError);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new DomainException("File exceeds 5MB limit", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new DomainException("Only PDF, JPEG, PNG, WEBP allowed", HttpStatus.BAD_REQUEST);
        }
    }
}
