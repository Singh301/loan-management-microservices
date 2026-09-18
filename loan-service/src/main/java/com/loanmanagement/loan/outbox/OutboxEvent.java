package com.loanmanagement.loan.outbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_retry", columnList = "status,next_retry_at,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "aggregate_id", nullable = false) private String aggregateId;
    @Column(name = "aggregate_type", nullable = false) private String aggregateType;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(nullable = false, columnDefinition = "JSON") private String payload;
    @Enumerated(EnumType.STRING) @Builder.Default private Status status = Status.PENDING;
    @Column(name = "retry_count", nullable = false) @Builder.Default private Integer retryCount = 0;
    @Column(name = "next_retry_at") private LocalDateTime nextRetryAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "created_at", updatable = false) @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "processed_at") private LocalDateTime processedAt;
    @Column(name = "processing_at") private LocalDateTime processingAt;

    public enum Status { PENDING, PROCESSING, PROCESSED, FAILED }
}
