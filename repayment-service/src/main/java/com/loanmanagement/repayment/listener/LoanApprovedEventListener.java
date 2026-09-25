package com.loanmanagement.repayment.listener;

import com.loanmanagement.common.event.DomainEvent;
import com.loanmanagement.common.event.LoanEventPayload;
import com.loanmanagement.common.event.LoanEvents;
import com.loanmanagement.repayment.service.EmiScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanApprovedEventListener {

    private final EmiScheduleService emiScheduleService;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            dltTopicSuffix = ".DLT")
    @KafkaListener(topics = "loan.events", groupId = "repayment-service")
    public void onLoanEvent(DomainEvent event) {
        if (!LoanEvents.LOAN_APPROVED.equals(event.getEventType())) {
            return;
        }
        if (!(event.getPayload() instanceof LoanEventPayload payload)) {
            throw new IllegalArgumentException("Invalid loan event payload");
        }
        if (payload.getLoanId() == null || payload.getLoanAmount() == null || payload.getInterestRate() == null
                || payload.getEmi() == null || payload.getTenureMonths() == null) {
            throw new IllegalArgumentException("Loan approval event is missing repayment schedule data");
        }

        log.info("Creating EMI schedule for approved loan {} from event {}", payload.getLoanId(), event.getEventId());
        emiScheduleService.generateSchedule(
                payload.getLoanId(),
                payload.getLoanAmount(),
                payload.getInterestRate(),
                payload.getTenureMonths(),
                payload.getEmi(),
                payload.getApplicationDate());
    }

    @DltHandler
    public void onDlt(DomainEvent event) {
        log.error("Loan approval event moved to DLT: eventId={}, aggregateId={}", event.getEventId(), event.getAggregateId());
    }
}
