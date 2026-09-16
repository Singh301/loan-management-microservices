-- Phase 2: reliability, idempotency and query-performance hardening.

ALTER TABLE outbox_events
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at DATETIME NULL,
    ADD COLUMN last_error VARCHAR(1000) NULL;

CREATE INDEX idx_outbox_status_retry_created
    ON outbox_events(status, next_retry_at, created_at);

CREATE TABLE idempotency_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    operation VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_idempotency_expiry (expires_at)
);

CREATE INDEX idx_loans_customer_status ON loans(customer_id, loan_status);
CREATE INDEX idx_loans_application_date ON loans(application_date);
CREATE INDEX idx_loans_product ON loans(product_id);
CREATE INDEX idx_loan_approvals_loan_level ON loan_approvals(loan_id, level);
CREATE INDEX idx_collaterals_loan ON collaterals(loan_id);
