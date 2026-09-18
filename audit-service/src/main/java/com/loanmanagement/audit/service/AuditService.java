package com.loanmanagement.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.audit.entity.AuditLog;
import com.loanmanagement.audit.repository.AuditLogRepository;
import com.loanmanagement.common.event.DomainEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains the managed repository and ObjectMapper beans.")
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(DomainEvent event) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Audit eventId is required");
        }

        try {
            String payload = objectMapper.writeValueAsString(event.getPayload());
            repository.insertIfAbsent(
                    event.getEventId(),
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getAggregateType(),
                    payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write audit log", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByAggregate(String aggregateId, Pageable pageable) {
        return repository.findByAggregateId(aggregateId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
