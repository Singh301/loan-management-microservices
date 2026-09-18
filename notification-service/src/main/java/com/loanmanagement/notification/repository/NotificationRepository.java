package com.loanmanagement.notification.repository;

import com.loanmanagement.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    java.util.Optional<Notification> findByEventId(String eventId);
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    java.util.Optional<Notification> findByIdAndCustomerId(Long id, Long customerId);
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO notifications
                (event_id, customer_id, loan_id, title, message, type, read_flag, created_at)
            VALUES
                (:eventId, :customerId, :loanId, :title, :message, :type, FALSE, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("customerId") Long customerId,
            @Param("loanId") Long loanId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("type") String type);
}
