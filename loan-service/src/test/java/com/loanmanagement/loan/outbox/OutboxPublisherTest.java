package com.loanmanagement.loan.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxRepository repository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JsonNode payload;

    @Test
    void shouldMarkEventProcessedAfterSuccessfulPublish() throws Exception {
        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper);

        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .aggregateId("42")
                .eventType("LOAN_APPROVED")
                .payload("{}")
                .retryCount(0)
                .status(OutboxEvent.Status.PENDING)
                .build();

        when(repository.findReady(any(LocalDateTime.class), eq(8), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(objectMapper.readTree("{}")).thenReturn(payload);
        when(kafkaTemplate.send("loan.events", "42", payload))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        assertEquals(OutboxEvent.Status.PROCESSED, event.getStatus());
        assertNull(event.getProcessingAt());
        assertNull(event.getLastError());
        verify(repository, atLeast(2)).save(event);
    }

    @Test
    void shouldStopRetryingAfterMaxRetries() {
        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate, objectMapper);

        OutboxEvent event = OutboxEvent.builder()
                .id(2L)
                .aggregateId("42")
                .eventType("LOAN_APPROVED")
                .payload("{}")
                .retryCount(7)
                .status(OutboxEvent.Status.FAILED)
                .build();

        when(repository.findReady(any(LocalDateTime.class), eq(8), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(event));
        when(objectMapper.readTree("{}")).thenReturn(payload);
        when(kafkaTemplate.send("loan.events", "42", payload))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        publisher.publishPendingEvents();

        assertEquals(8, event.getRetryCount());
        assertEquals(OutboxEvent.Status.FAILED, event.getStatus());
        assertNull(event.getNextRetryAt());
        assertNull(event.getProcessingAt());
        verify(repository, atLeast(2)).save(event);
    }
}
