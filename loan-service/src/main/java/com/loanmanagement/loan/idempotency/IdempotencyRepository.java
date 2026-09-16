package com.loanmanagement.loan.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKeyAndOperation(String idempotencyKey, String operation);
    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
