package com.loanmanagement.loan.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.common.exception.DomainException;
import com.loanmanagement.loan.dto.LoanResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private static final String APPLY_OPERATION = "LOAN_APPLY";
    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public String normalizeKey(String key) {
        if (key == null || key.isBlank()) throw new DomainException("Idempotency-Key header is required", HttpStatus.BAD_REQUEST);
        String normalized = key.trim();
        if (normalized.length() < 8 || normalized.length() > 100) throw new DomainException("Idempotency-Key must contain between 8 and 100 characters", HttpStatus.BAD_REQUEST);
        return normalized;
    }

    @Transactional(readOnly = true)
    public LoanResponseDto findExisting(String key, String requestHash) {
        return repository.findByIdempotencyKeyAndOperation(key, APPLY_OPERATION).filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now())).map(r -> {
            if (!r.getRequestHash().equals(requestHash)) throw new DomainException("Idempotency-Key was already used with a different request", HttpStatus.CONFLICT);
            if (r.getStatus() != IdempotencyRecord.Status.COMPLETED || r.getResponseBody() == null) throw new DomainException("Request with this Idempotency-Key is already in progress", HttpStatus.CONFLICT);
            try { return objectMapper.readValue(r.getResponseBody(), LoanResponseDto.class); }
            catch (Exception e) { throw new DomainException("Unable to restore idempotent response", HttpStatus.INTERNAL_SERVER_ERROR); }
        }).orElse(null);
    }

    @Transactional
    public void claim(String key, String requestHash) {
        try {
            repository.saveAndFlush(IdempotencyRecord.builder().idempotencyKey(key).operation(APPLY_OPERATION)
                    .requestHash(requestHash).status(IdempotencyRecord.Status.PROCESSING).expiresAt(LocalDateTime.now().plusHours(24)).build());
        } catch (DataIntegrityViolationException e) {
            throw new DomainException("Request with this Idempotency-Key is already in progress or completed", HttpStatus.CONFLICT);
        }
    }

    @Transactional
    public void complete(String key, LoanResponseDto response) {
        repository.findByIdempotencyKeyAndOperation(key, APPLY_OPERATION).ifPresent(record -> {
            try {
                record.setResponseBody(objectMapper.writeValueAsString(response));
                record.setStatus(IdempotencyRecord.Status.COMPLETED);
                repository.save(record);
            } catch (Exception e) { throw new DomainException("Unable to persist idempotent response", HttpStatus.INTERNAL_SERVER_ERROR); }
        });
    }

    public String requestHash(Object request) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(request))); }
        catch (Exception e) { throw new DomainException("Unable to calculate request hash", HttpStatus.INTERNAL_SERVER_ERROR); }
    }
}
