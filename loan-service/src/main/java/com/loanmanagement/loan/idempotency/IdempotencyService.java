package com.loanmanagement.loan.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.loan.dto.LoanResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    public static final String APPLY_OPERATION = "LOAN_APPLY";
    public static final String DISBURSE_OPERATION = "LOAN_DISBURSE";
    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException(
                    "Idempotency-Key header is required",
                    HttpStatus.BAD_REQUEST);
        }
        String normalized = key.trim();
        if (normalized.length() < 8 || normalized.length() > 100) {
            throw new DomainException(
                    "Idempotency-Key must contain between 8 and 100 characters",
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public LoanResponseDto findExisting(String key, String requestHash) {
        return findExisting(key, requestHash, APPLY_OPERATION);
    }

    @Transactional(readOnly = true)
    public LoanResponseDto findExisting(String key, String requestHash, String operation) {
        return repository.findByIdempotencyKeyAndOperation(key, operation)
                .filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(r -> {
                    if (!r.getRequestHash().equals(requestHash)) {
                        throw new DomainException(
                                "Idempotency-Key was already used with a different request",
                                HttpStatus.CONFLICT);
                    }
                    if (r.getStatus() != IdempotencyRecord.Status.COMPLETED
                            || r.getResponseBody() == null) {
                        throw new DomainException(
                                "Request with this Idempotency-Key is already in progress",
                                HttpStatus.CONFLICT);
                    }
                    try {
                        return objectMapper.readValue(r.getResponseBody(), LoanResponseDto.class);
                    } catch (Exception e) {
                        throw new DomainException(
                                "Unable to restore idempotent response",
                                HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                })
                .orElse(null);
    }

    @Transactional
    public void claim(String key, String requestHash) {
        claim(key, requestHash, APPLY_OPERATION);
    }

    @Transactional
    public void claim(String key, String requestHash, String operation) {
        try {
            repository.saveAndFlush(IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .operation(operation)
                    .requestHash(requestHash)
                    .status(IdempotencyRecord.Status.PROCESSING)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new DomainException(
                    "Request with this Idempotency-Key is already in progress or completed",
                    HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void complete(String key, LoanResponseDto response) {
        complete(key, response, APPLY_OPERATION);
    }

    @Transactional
    public void complete(String key, LoanResponseDto response, String operation) {
        repository.findByIdempotencyKeyAndOperation(key, operation).ifPresent(record -> {
            try {
                record.setResponseBody(objectMapper.writeValueAsString(response));
                record.setStatus(IdempotencyRecord.Status.COMPLETED);
                repository.save(record);
            } catch (Exception e) {
                throw new DomainException(
                        "Unable to persist idempotent response",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Scheduled(cron = "0 45 2 * * *")
    @Transactional
    public void cleanupExpiredRecords() {
        long deleted = repository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            org.slf4j.LoggerFactory.getLogger(IdempotencyService.class)
                    .info("Deleted {} expired idempotency records", deleted);
        }
    }

    public String requestHash(Object request) {
        try {
            byte[] serializedRequest = objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(serializedRequest);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new DomainException(
                    "Unable to calculate request hash",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
