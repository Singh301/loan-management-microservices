package com.loanmanagement.audit.repository;

import com.loanmanagement.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByAggregateId(String aggregateId, Pageable pageable);
    Page<AuditLog> findByEventType(String eventType, Pageable pageable);
}
