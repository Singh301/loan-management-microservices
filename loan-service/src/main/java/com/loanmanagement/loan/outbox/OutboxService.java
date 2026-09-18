package com.loanmanagement.loan.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.common.event.DomainEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring dependency injection intentionally retains managed repository and ObjectMapper beans.")
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(DomainEvent event) {
        try {
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateId(event.getAggregateId())
                    .aggregateType(event.getAggregateType())
                    .eventType(event.getEventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxEvent.Status.PENDING)
                    .build();
            outboxRepository.save(outbox);
            log.debug(
                    "Outbox event enqueued: type={}, aggregate={}",
                    event.getEventType(),
                    event.getAggregateId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue outbox event", e);
        }
    }
}
