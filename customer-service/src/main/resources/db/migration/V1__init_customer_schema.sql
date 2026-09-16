CREATE TABLE IF NOT EXISTS customers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT         NULL,
    full_name       VARCHAR(200)   NOT NULL,
    email           VARCHAR(255)   NOT NULL UNIQUE,
    phone           VARCHAR(20)    NOT NULL,
    date_of_birth   DATE           NULL,
    address         VARCHAR(500)   NULL,
    city            VARCHAR(100)   NULL,
    state           VARCHAR(100)   NULL,
    pincode         VARCHAR(10)    NULL,
    pan_number      VARCHAR(20)    UNIQUE,
    aadhaar_number  VARCHAR(20)    UNIQUE,
    kyc_status      VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME       NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_user ON customers(user_id);
