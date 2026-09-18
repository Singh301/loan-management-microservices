ALTER TABLE audit_logs
    ADD COLUMN event_id VARCHAR(64) NULL;

UPDATE audit_logs
SET event_id = CONCAT('legacy-', id)
WHERE event_id IS NULL;

ALTER TABLE audit_logs
    MODIFY COLUMN event_id VARCHAR(64) NOT NULL;

ALTER TABLE audit_logs
    ADD CONSTRAINT uk_audit_event_id UNIQUE (event_id);
