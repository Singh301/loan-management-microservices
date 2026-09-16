package com.loanmanagement.audit.listener;

import com.loanmanagement.audit.service.AuditService;
import com.loanmanagement.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditService auditService;

    @KafkaListener(topics = "loan.events", groupId = "audit-service")
    public void onEvent(DomainEvent event) {
        log.debug("Auditing event: {}", event.getEventType());
        auditService.record(event);
    }
}
