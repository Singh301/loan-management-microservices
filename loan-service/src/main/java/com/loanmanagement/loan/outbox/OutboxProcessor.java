package com.loanmanagement.loan.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final String TOPIC = "loan.events";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pending = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                DomainEvent domainEvent = objectMapper.readValue(event.getPayload(), DomainEvent.class);
                kafkaTemplate.send(TOPIC, event.getAggregateId(), domainEvent).get();
                event.setStatus(OutboxEvent.Status.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                log.debug("Published outbox event {} to Kafka", event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}", event.getId(), e);
                event.setStatus(OutboxEvent.Status.FAILED);
            }
        }
        outboxRepository.saveAll(pending);
    }
}
