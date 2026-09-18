package com.loanmanagement.audit.repository;

import com.loanmanagement.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByAggregateId(String aggregateId, Pageable pageable);
    Page<AuditLog> findByEventType(String eventType, Pageable pageable);
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO audit_logs
                (event_id, event_type, aggregate_id, aggregate_type, payload, created_at)
            VALUES
                (:eventId, :eventType, :aggregateId, :aggregateType, :payload, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("aggregateId") String aggregateId,
            @Param("aggregateType") String aggregateType,
            @Param("payload") String payload);
}
