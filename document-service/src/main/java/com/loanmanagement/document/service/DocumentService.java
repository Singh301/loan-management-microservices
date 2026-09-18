package com.loanmanagement.document.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.document.entity.Document;
import com.loanmanagement.document.repository.DocumentRepository;
import com.loanmanagement.document.storage.DocumentStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains managed repository and storage beans.")
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
        if (file == null) {
            throw new DomainException("File is empty", HttpStatus.BAD_REQUEST);
        }
        validate(file);

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "file";
        }
        Path originalPath = Paths.get(original.replace("\\", "/")).getFileName();
        if (originalPath == null) {
            throw new DomainException("Invalid file name", HttpStatus.BAD_REQUEST);
        }
        String safeOriginal = originalPath.toString();

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

    private boolean matchesSignature(String contentType, byte[] header) {
        return switch (contentType) {
            case "application/pdf" ->
                    header.length >= 5
                            && header[0] == '%'
                            && header[1] == 'P'
                            && header[2] == 'D'
                            && header[3] == 'F'
                            && header[4] == '-';
            case "image/jpeg" ->
                    header.length >= 3
                            && (header[0] & 0xFF) == 0xFF
                            && (header[1] & 0xFF) == 0xD8
                            && (header[2] & 0xFF) == 0xFF;
            case "image/png" ->
                    header.length >= 8
                            && (header[0] & 0xFF) == 0x89
                            && (header[1] & 0xFF) == 0x50
                            && (header[2] & 0xFF) == 0x4E
                            && (header[3] & 0xFF) == 0x47
                            && (header[4] & 0xFF) == 0x0D
                            && (header[5] & 0xFF) == 0x0A
                            && (header[6] & 0xFF) == 0x1A
                            && (header[7] & 0xFF) == 0x0A;
            case "image/webp" ->
                    header.length >= 12
                            && header[0] == 'R'
                            && header[1] == 'I'
                            && header[2] == 'F'
                            && header[3] == 'F'
                            && header[8] == 'W'
                            && header[9] == 'E'
                            && header[10] == 'B'
                            && header[11] == 'P';
            default -> false;
        };
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

        try (java.io.InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            if (!matchesSignature(contentType, header)) {
                throw new DomainException("File content does not match declared type", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException ex) {
            throw new DomainException("Unable to validate uploaded file", HttpStatus.BAD_REQUEST);
        }
    }
}
