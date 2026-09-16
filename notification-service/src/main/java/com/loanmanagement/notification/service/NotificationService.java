package com.loanmanagement.notification.service;

import com.loanmanagement.notification.entity.Notification;
import com.loanmanagement.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional
    public Notification create(Long customerId, Long loanId, String title, String message, String type) {
        Notification n = Notification.builder()
                .customerId(customerId)
                .loanId(loanId)
                .title(title)
                .message(message)
                .type(type)
                .build();
        return repository.save(n);
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
