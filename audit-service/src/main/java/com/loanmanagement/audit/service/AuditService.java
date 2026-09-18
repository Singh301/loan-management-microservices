package com.loanmanagement.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.audit.entity.AuditLog;
import com.loanmanagement.audit.repository.AuditLogRepository;
import com.loanmanagement.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(DomainEvent event) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Audit eventId is required");
        }

        if (repository.existsByEventId(event.getEventId())) {
            return;
        }

        try {
            AuditLog log = AuditLog.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .aggregateId(event.getAggregateId())
                    .aggregateType(event.getAggregateType())
                    .payload(objectMapper.writeValueAsString(event.getPayload()))
                    .build();
            repository.saveAndFlush(log);
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            // Another consumer instance may have inserted the same event concurrently.
            if (!repository.existsByEventId(event.getEventId())) {
                throw new RuntimeException("Failed to write audit log", duplicate);
            }
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
