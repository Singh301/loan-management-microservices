package com.loanmanagement.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.audit.repository.AuditLogRepository;
import com.loanmanagement.common.event.DomainEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceIdempotencyTest {

    @Mock
    private AuditLogRepository repository;

    @Test
    void shouldIgnoreDuplicateEventUsingAtomicInsert() {
        AuditService service = new AuditService(repository, new ObjectMapper());
        DomainEvent event = DomainEvent.of("LOAN_APPROVED", "42", "Loan", java.util.Map.of("loanId", 42));

        when(repository.insertIfAbsent(
                eq(event.getEventId()),
                eq(event.getEventType()),
                eq(event.getAggregateId()),
                eq(event.getAggregateType()),
                anyString())).thenReturn(0);

        service.record(event);

        verify(repository).insertIfAbsent(
                eq(event.getEventId()),
                eq(event.getEventType()),
                eq(event.getAggregateId()),
                eq(event.getAggregateType()),
                anyString());
    }
}
