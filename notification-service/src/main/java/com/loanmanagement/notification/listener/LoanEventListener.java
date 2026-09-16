package com.loanmanagement.notification.listener;

import com.loanmanagement.common.event.DomainEvent;
import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.common.event.LoanEvents;
import com.loanmanagement.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "loan.events", groupId = "notification-service")
    public void onLoanEvent(DomainEvent event) {
        if (event == null || event.getEventId() == null) {
            throw new IllegalArgumentException("Kafka loan event must contain eventId");
        }
        log.info("Received loan event eventId={}, type={}, aggregateId={}",
                event.getEventId(), event.getEventType(), event.getAggregateId());

        if (!(event.getPayload() instanceof LoanEventPayload payload)) {
            log.warn("Ignoring unsupported loan event payload. eventId={}", event.getEventId());
            return;
        }

        String title;
        String message;
        switch (event.getEventType()) {
            case LoanEvents.LOAN_APPLIED -> {
                title = "Loan Application Received";
                message = "Your loan application #" + payload.getLoanId() + " has been submitted successfully.";
            }
            case LoanEvents.LOAN_APPROVED -> {
                title = "Loan Approved";
                message = "Congratulations! Loan #" + payload.getLoanId() + " has been approved. EMI: " + payload.getEmi();
            }
            case LoanEvents.LOAN_REJECTED -> {
                title = "Loan Rejected";
                message = "Loan #" + payload.getLoanId() + " was rejected. Reason: " + payload.getRemarks();
            }
            case LoanEvents.LOAN_DISBURSED -> {
                title = "Loan Disbursed";
                message = "Loan #" + payload.getLoanId() + " has been disbursed on " + payload.getDisbursementDate();
            }
            default -> {
                return;
            }
        }

        notificationService.create(event.getEventId(), payload.getCustomerId(), payload.getLoanId(),
                title, message, event.getEventType());
    }
}
