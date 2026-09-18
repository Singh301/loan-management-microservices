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
        int inserted = repository.insertIfAbsent(
                eventId, customerId, loanId, title, message, type);
        if (inserted == 0) {
            log.info("Ignoring duplicate notification event {}", eventId);
            return null;
        }
        return repository.findByEventId(eventId).orElse(null);
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

    @Transactional
    public void markReadForCustomer(Long id, Long customerId) {
        repository.findByIdAndCustomerId(id, customerId).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }
}
