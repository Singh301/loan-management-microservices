ALTER TABLE documents
    ADD COLUMN verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN verified_by VARCHAR(100) NULL,
    ADD COLUMN verified_at DATETIME NULL,
    ADD COLUMN verification_remarks VARCHAR(500) NULL;

CREATE INDEX idx_doc_verification_status ON documents(verification_status);
