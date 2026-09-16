package com.loanmanagement.notification.service;

import com.loanmanagement.notification.entity.Notification;
import com.loanmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public Notification create(String eventId, Long customerId, Long loanId, String title, String message, String type) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event ID is required");
        }
        if (repository.existsByEventId(eventId)) {
            log.info("Ignoring duplicate notification event {}", eventId);
            return null;
        }
        Notification n = Notification.builder()
                .eventId(eventId)
                .customerId(customerId)
                .loanId(loanId)
                .title(title)
                .message(message)
                .type(type)
                .build();
        return repository.saveAndFlush(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> getByCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public void markRead(Long id) {
        repository.findById(id).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }
}
