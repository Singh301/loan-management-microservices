package com.loanmanagement.notification.service;

import com.loanmanagement.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceIdempotencyTest {

    @Mock
    private NotificationRepository repository;

    @Test
    void shouldIgnoreDuplicateEventAtomically() {
        NotificationService service = new NotificationService(repository);

        when(repository.insertIfAbsent(
                eq("event-1"), eq(10L), eq(20L),
                eq("Loan Approved"), eq("Loan approved"), eq("LOAN_APPROVED")))
                .thenReturn(0);

        service.create("event-1", 10L, 20L,
                "Loan Approved", "Loan approved", "LOAN_APPROVED");

        verify(repository).insertIfAbsent(
                "event-1", 10L, 20L,
                "Loan Approved", "Loan approved", "LOAN_APPROVED");
    }
}
