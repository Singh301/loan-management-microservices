package com.loanmanagement.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_customer_created", columnList = "customer_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_notifications_event_id", columnNames = "event_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "read_flag")
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
