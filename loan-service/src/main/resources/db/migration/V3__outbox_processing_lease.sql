ALTER TABLE outbox_events
    ADD COLUMN processing_at DATETIME NULL;

CREATE INDEX idx_outbox_processing_lease
    ON outbox_events(status, processing_at, created_at);
