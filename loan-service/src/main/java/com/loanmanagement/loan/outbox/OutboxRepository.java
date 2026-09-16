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
    @Query("select e from OutboxEvent e where e.status in :statuses and (e.nextRetryAt is null or e.nextRetryAt <= :now) order by e.createdAt asc")
    List<OutboxEvent> findReady(@Param("statuses") List<OutboxEvent.Status> statuses, @Param("now") LocalDateTime now, Pageable pageable);

    long deleteByStatusAndProcessedAtBefore(OutboxEvent.Status status, LocalDateTime cutoff);
}
