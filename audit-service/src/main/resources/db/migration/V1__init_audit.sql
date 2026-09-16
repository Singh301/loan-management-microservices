CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type      VARCHAR(100)  NOT NULL,
    aggregate_id    VARCHAR(100)  NULL,
    aggregate_type  VARCHAR(100)  NULL,
    actor           VARCHAR(100)  NULL,
    payload         JSON          NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_type ON audit_logs(event_type);
CREATE INDEX idx_audit_aggregate ON audit_logs(aggregate_id);
