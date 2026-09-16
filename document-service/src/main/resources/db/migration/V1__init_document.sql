CREATE TABLE IF NOT EXISTS documents (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id         BIGINT        NULL,
    customer_id     BIGINT        NULL,
    document_type   VARCHAR(50)   NOT NULL,
    original_name   VARCHAR(255)  NOT NULL,
    stored_name     VARCHAR(255)  NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    size_bytes      BIGINT        NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    uploaded_by     VARCHAR(100)  NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doc_loan ON documents(loan_id);
CREATE INDEX idx_doc_customer ON documents(customer_id);
