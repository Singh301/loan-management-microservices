package com.loanmanagement.loan.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private static final int PROCESSING_LEASE_MINUTES = 2;
    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @jakarta.annotation.PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("loan_outbox_pending_events", this, publisher -> repository.countByStatus(OutboxEvent.Status.PENDING));
        meterRegistry.gauge("loan_outbox_failed_events", this, publisher -> repository.countByStatus(OutboxEvent.Status.FAILED));
        meterRegistry.gauge("loan_outbox_processing_events", this, publisher -> repository.countByStatus(OutboxEvent.Status.PROCESSING));
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:2000}")
    public void publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEvent> events = repository.findReady(
                now,
                MAX_RETRIES,
                now.minusMinutes(PROCESSING_LEASE_MINUTES),
                OutboxEvent.Status.PENDING,
                OutboxEvent.Status.FAILED,
                OutboxEvent.Status.PROCESSING,
                PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : events) {
            int claimed = repository.claimForProcessing(
                    event.getId(),
                    now,
                    MAX_RETRIES,
                    now.minusMinutes(PROCESSING_LEASE_MINUTES),
                    OutboxEvent.Status.PENDING,
                    OutboxEvent.Status.FAILED,
                    OutboxEvent.Status.PROCESSING);
            if (claimed == 1) {
                publish(event);
            }
        }
    }

    private void publish(OutboxEvent event) {
        try {
            event.setStatus(OutboxEvent.Status.PROCESSING);
            event.setProcessingAt(LocalDateTime.now());
            JsonNode payload = objectMapper.readTree(event.getPayload());
            kafkaTemplate.send(TOPIC, event.getAggregateId(), payload).get(10, TimeUnit.SECONDS);
            event.setStatus(OutboxEvent.Status.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            event.setProcessingAt(null);
            event.setLastError(null);
            repository.save(event);
        } catch (Exception ex) {
            int retries = event.getRetryCount() == null ? 0 : event.getRetryCount(); retries++;
            event.setRetryCount(retries);
            event.setLastError(trim(ex.getMessage()));
            event.setStatus(OutboxEvent.Status.FAILED);
            event.setProcessingAt(null);
            if (retries < MAX_RETRIES) event.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(3600, 1L << Math.min(retries, 10))));
            else event.setNextRetryAt(null);
            repository.save(event);
            if (retries >= MAX_RETRIES) log.error("Outbox event permanently failed: id={}, type={}, retries={}", event.getId(), event.getEventType(), retries, ex);
            else log.warn("Outbox publish failed: id={}, retry={}, nextRetryAt={}", event.getId(), retries, event.getNextRetryAt());
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void cleanupProcessedEvents() {
        long deleted = repository.deleteByStatusAndProcessedAtBefore(OutboxEvent.Status.PROCESSED, LocalDateTime.now().minusDays(7));
        if (deleted > 0) log.info("Deleted {} processed outbox events older than 7 days", deleted);
    }

    private String trim(String message) { if (message == null) return "Unknown publishing error"; return message.length() <= 1000 ? message : message.substring(0, 1000); }
}
