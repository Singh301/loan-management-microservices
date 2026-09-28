-- Ensure the refresh-token table exists even when an older deployment
-- created a partial schema before Flyway was introduced.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(500)  NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    expiry_date     DATETIME      NOT NULL,
    revoked         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Replace plaintext refresh tokens with SHA-256 hashes.
UPDATE refresh_tokens
SET token = SHA2(token, 256)
WHERE token IS NOT NULL
  AND CHAR_LENGTH(token) <> 64;

ALTER TABLE refresh_tokens
    MODIFY COLUMN token VARCHAR(64) NOT NULL;

CREATE INDEX idx_refresh_tokens_expiry_date
    ON refresh_tokens(expiry_date);