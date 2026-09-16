CREATE TABLE IF NOT EXISTS emi_schedules (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id             BIGINT         NOT NULL,
    installment_number  INT            NOT NULL,
    due_date            DATE           NOT NULL,
    principal_component DECIMAL(15,2)  NOT NULL,
    interest_component  DECIMAL(15,2)  NOT NULL,
    emi_amount          DECIMAL(15,2)  NOT NULL,
    late_fee            DECIMAL(15,2)  NOT NULL DEFAULT 0,
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    paid_date           DATE           NULL,
    paid_amount         DECIMAL(15,2)  NULL,
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_loan_installment (loan_id, installment_number)
);

CREATE TABLE IF NOT EXISTS repayments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id             BIGINT         NOT NULL,
    emi_schedule_id     BIGINT         NULL,
    amount              DECIMAL(15,2)  NOT NULL,
    payment_mode        VARCHAR(50)    NOT NULL,
    transaction_ref     VARCHAR(100)   NULL,
    payment_date        DATE           NOT NULL,
    remarks             VARCHAR(500)   NULL,
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repayment_emi FOREIGN KEY (emi_schedule_id) REFERENCES emi_schedules(id)
);

CREATE INDEX idx_emi_loan ON emi_schedules(loan_id);
CREATE INDEX idx_emi_due ON emi_schedules(due_date);
CREATE INDEX idx_emi_status ON emi_schedules(status);
CREATE INDEX idx_repayment_loan ON repayments(loan_id);
