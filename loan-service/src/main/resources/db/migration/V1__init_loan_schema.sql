CREATE TABLE IF NOT EXISTS loan_products (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code            VARCHAR(50)    NOT NULL UNIQUE,
    product_name            VARCHAR(150)   NOT NULL,
    loan_type               VARCHAR(50)    NOT NULL,
    interest_rate           DECIMAL(5,2)   NOT NULL,
    min_tenure              INT            NOT NULL,
    max_tenure              INT            NOT NULL,
    min_amount              DECIMAL(15,2)  NOT NULL,
    max_amount              DECIMAL(15,2)  NOT NULL,
    processing_fee_percent  DECIMAL(10,2)  NULL,
    late_fee_amount         DECIMAL(10,2)  NULL,
    active                  BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loans (
    loan_id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id                     BIGINT         NOT NULL,
    product_id                      BIGINT         NULL,
    loan_type                       VARCHAR(50)    NOT NULL,
    loan_amount                     DECIMAL(15,2)  NOT NULL,
    interest_rate                   DECIMAL(5,2)   NOT NULL,
    tenure_months                   INT            NOT NULL,
    emi                             DECIMAL(15,2)  NULL,
    loan_status                     VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    application_date                DATE           NOT NULL,
    remarks                         VARCHAR(500)   NULL,
    outstanding_principal           DECIMAL(18,2)  NOT NULL DEFAULT 0,
    paid_installments               INT            NOT NULL DEFAULT 0,
    remaining_installments          INT            NOT NULL DEFAULT 0,
    disbursement_date               DATE           NULL,
    next_due_date                   DATE           NULL,
    total_late_fee                  DECIMAL(15,2)  NOT NULL DEFAULT 0,
    disbursement_idempotency_key    VARCHAR(100)   NULL,
    level1_approved_by              VARCHAR(100)   NULL,
    level1_approved_at              DATETIME       NULL,
    level2_approved_by              VARCHAR(100)   NULL,
    level2_approved_at              DATETIME       NULL,
    deleted                         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at                      DATETIME       NULL,
    version                         BIGINT         NOT NULL DEFAULT 0,
    created_at                      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_disbursement_idempotency UNIQUE (disbursement_idempotency_key)
);

CREATE TABLE IF NOT EXISTS loan_approvals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id         BIGINT       NOT NULL,
    level           INT          NOT NULL,
    approved_by     VARCHAR(100) NOT NULL,
    decision        VARCHAR(20)  NOT NULL,
    remarks         VARCHAR(500) NULL,
    decided_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_loan FOREIGN KEY (loan_id) REFERENCES loans(loan_id)
);

CREATE TABLE IF NOT EXISTS collaterals (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id           BIGINT         NOT NULL,
    collateral_type   VARCHAR(100)   NOT NULL,
    description       VARCHAR(500)   NULL,
    estimated_value   DECIMAL(15,2)  NOT NULL,
    ownership_proof   VARCHAR(255)   NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collateral_loan FOREIGN KEY (loan_id) REFERENCES loans(loan_id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id    VARCHAR(100)  NOT NULL,
    aggregate_type  VARCHAR(100)  NOT NULL,
    event_type      VARCHAR(100)  NOT NULL,
    payload         JSON          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at    DATETIME      NULL
);

CREATE INDEX idx_loans_customer ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(loan_status);
CREATE INDEX idx_outbox_status ON outbox_events(status);

INSERT INTO loan_products (product_code, product_name, loan_type, interest_rate, min_tenure, max_tenure, min_amount, max_amount, processing_fee_percent, late_fee_amount) VALUES
('PERSONAL_STD', 'Personal Loan Standard', 'PERSONAL', 12.50, 12, 60, 50000, 1000000, 1.00, 500.00),
('HOME_PREM', 'Home Loan Premium', 'HOME', 8.75, 60, 360, 500000, 10000000, 0.50, 1000.00),
('AUTO_STD', 'Auto Loan', 'AUTO', 9.50, 12, 84, 100000, 2000000, 1.00, 750.00);
