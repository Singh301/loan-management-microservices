-- Repair the refresh-token schema for databases that were partially initialized
-- before all authentication migrations were applied.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(64)   NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    expiry_date     DATETIME      NOT NULL,
    revoked         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);