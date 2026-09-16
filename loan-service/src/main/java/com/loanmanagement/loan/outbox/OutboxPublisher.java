package com.loanmanagement.loan.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private static final String TOPIC = "loan.events";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 8;

    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = repository.findReady(LocalDateTime.now(), PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : events) publish(event);
    }

    private void publish(OutboxEvent event) {
        try {
            event.setStatus(OutboxEvent.Status.PROCESSING);
            repository.save(event);
            JsonNode payload = objectMapper.readTree(event.getPayload());
            kafkaTemplate.send(TOPIC, event.getAggregateId(), payload).get(10, TimeUnit.SECONDS);
            event.setStatus(OutboxEvent.Status.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            repository.save(event);
        } catch (Exception ex) {
            int retries = event.getRetryCount() == null ? 0 : event.getRetryCount();
            retries++;
            event.setRetryCount(retries);
            event.setLastError(trim(ex.getMessage()));
            if (retries >= MAX_RETRIES) {
                event.setStatus(OutboxEvent.Status.FAILED);
                event.setNextRetryAt(null);
                log.error("Outbox event permanently failed: id={}, type={}, retries={}", event.getId(), event.getEventType(), retries, ex);
            } else {
                event.setStatus(OutboxEvent.Status.FAILED);
                long delaySeconds = Math.min(3600, 1L << Math.min(retries, 10));
                event.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
                log.warn("Outbox publish failed: id={}, retry={}, nextRetryAt={}", event.getId(), retries, event.getNextRetryAt());
            }
            repository.save(event);
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupProcessedEvents() {
        long deleted = repository.deleteByStatusAndProcessedAtBefore(
                OutboxEvent.Status.PROCESSED, LocalDateTime.now().minusDays(7));
        if (deleted > 0) log.info("Deleted {} processed outbox events older than 7 days", deleted);
    }

    private String trim(String message) {
        if (message == null) return "Unknown publishing error";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
