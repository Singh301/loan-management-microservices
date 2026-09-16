package com.loanmanagement.document.service;

import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.common.exception.ResourceNotFoundException;
import com.loanmanagement.document.entity.Document;
import com.loanmanagement.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private final DocumentRepository documentRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional
    public Document upload(MultipartFile file, Long loanId, Long customerId,
                           String documentType, String uploadedBy) {
        validate(file);
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String safeOriginal = Paths.get(original).getFileName().toString();
            String stored = UUID.randomUUID() + "_" + safeOriginal;
            Path target = dir.resolve(stored).normalize();
            if (!target.getParent().equals(dir)) {
                throw new DomainException("Invalid storage path", HttpStatus.BAD_REQUEST);
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Document doc = Document.builder()
                    .loanId(loanId)
                    .customerId(customerId)
                    .documentType(documentType)
                    .originalName(safeOriginal)
                    .storedName(stored)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storagePath(target.toString())
                    .uploadedBy(uploadedBy)
                    .build();

            return documentRepository.save(doc);
        } catch (IOException e) {
            throw new DomainException("Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public Resource download(Long id) {
        Document doc = getMeta(id);
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path path = Paths.get(doc.getStoragePath()).toAbsolutePath().normalize();
            if (!path.startsWith(base)) {
                throw new DomainException("Invalid file path", HttpStatus.FORBIDDEN);
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new DomainException("File not readable", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new DomainException("Invalid file path", HttpStatus.INTERNAL_SERVER_ERROR);
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
            throw new DomainException("Verification status must be VERIFIED or REJECTED", HttpStatus.BAD_REQUEST);
        }
        Document document = getMeta(id);
        document.setVerificationStatus(status);
        document.setVerificationRemarks(remarks);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        return documentRepository.save(document);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new DomainException("File exceeds 5MB limit", HttpStatus.BAD_REQUEST);
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct)) {
            throw new DomainException("Only PDF, JPEG, PNG, WEBP allowed", HttpStatus.BAD_REQUEST);
        }
    }
}
