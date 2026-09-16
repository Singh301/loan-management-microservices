CREATE TABLE IF NOT EXISTS notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NULL,
    customer_id     BIGINT        NULL,
    loan_id         BIGINT        NULL,
    title           VARCHAR(200)  NOT NULL,
    message         VARCHAR(1000) NOT NULL,
    type            VARCHAR(50)   NOT NULL,
    read_flag       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_customer ON notifications(customer_id);
