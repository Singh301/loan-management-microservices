package com.loanmanagement.loan.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("""
            select e from OutboxEvent e
            where
                (e.status = 'PENDING'
                    and (e.nextRetryAt is null or e.nextRetryAt <= :now))
                or
                (e.status = 'FAILED'
                    and e.retryCount < :maxRetries
                    and (e.nextRetryAt is null or e.nextRetryAt <= :now))
                or
                (e.status = 'PROCESSING'
                    and e.processingAt is not null
                    and e.processingAt <= :staleBefore)
            order by e.createdAt asc
            """)
    List<OutboxEvent> findReady(
            @Param("now") LocalDateTime now,
            @Param("maxRetries") int maxRetries,
            @Param("staleBefore") LocalDateTime staleBefore,
            Pageable pageable);

    long deleteByStatusAndProcessedAtBefore(OutboxEvent.Status status, LocalDateTime cutoff);
    long countByStatus(OutboxEvent.Status status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
            update OutboxEvent e
            set e.status = 'PROCESSING',
                e.processingAt = :now,
                e.nextRetryAt = null
            where e.id = :id
              and (
                    (e.status = 'PENDING'
                        and (e.nextRetryAt is null or e.nextRetryAt <= :now))
                    or
                    (e.status = 'FAILED'
                        and e.retryCount < :maxRetries
                        and (e.nextRetryAt is null or e.nextRetryAt <= :now))
                    or
                    (e.status = 'PROCESSING'
                        and e.processingAt is not null
                        and e.processingAt <= :staleBefore)
                  )
            """)
    int claimForProcessing(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("now") LocalDateTime now,
            @org.springframework.data.repository.query.Param("maxRetries") int maxRetries,
            @org.springframework.data.repository.query.Param("staleBefore") LocalDateTime staleBefore);
}
