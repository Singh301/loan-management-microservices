package com.loanmanagement.loan.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.loan.dto.LoanResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String APPLY_OPERATION = "LOAN_APPLY";
    private static final int KEY_MIN_LENGTH = 8;
    private static final int KEY_MAX_LENGTH = 100;

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException("Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        }
        String normalized = key.trim();
        if (normalized.length() < KEY_MIN_LENGTH || normalized.length() > KEY_MAX_LENGTH) {
            throw new DomainException("Idempotency-Key must contain between 8 and 100 characters", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public LoanResponseDto findExisting(String key, String requestHash) {
        return repository.findByIdempotencyKeyAndOperation(key, APPLY_OPERATION)
                .filter(record -> record.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(record -> {
                    if (!record.getRequestHash().equals(requestHash)) {
                        throw new DomainException("Idempotency-Key was already used with a different request", HttpStatus.CONFLICT);
                    }
                    try {
                        return objectMapper.readValue(record.getResponseBody(), LoanResponseDto.class);
                    } catch (Exception e) {
                        throw new DomainException("Unable to restore idempotent response", HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                })
                .orElse(null);
    }

    @Transactional
    public void save(String key, String requestHash, LoanResponseDto response) {
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .operation(APPLY_OPERATION)
                    .requestHash(requestHash)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();
            repository.save(record);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Another concurrent request won the unique-key race. Its response will be reused on retry.
        } catch (Exception e) {
            throw new DomainException("Unable to persist idempotency record", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String requestHash(Object request) {
        try {
            byte[] canonical = objectMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
        } catch (Exception e) {
            throw new DomainException("Unable to calculate request hash", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
