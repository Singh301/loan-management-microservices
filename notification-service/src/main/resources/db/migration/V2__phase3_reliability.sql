ALTER TABLE notifications
    ADD COLUMN event_id VARCHAR(64) NULL;

UPDATE notifications
SET event_id = CONCAT('legacy-', id)
WHERE event_id IS NULL;

ALTER TABLE notifications
    MODIFY COLUMN event_id VARCHAR(64) NOT NULL;

ALTER TABLE notifications
    ADD CONSTRAINT uk_notifications_event_id UNIQUE (event_id);

CREATE INDEX idx_notifications_customer_created
    ON notifications (customer_id, created_at);
