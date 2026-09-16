CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(100)  NOT NULL UNIQUE,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    full_name       VARCHAR(200)  NOT NULL,
    role            VARCHAR(50)   NOT NULL,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    account_locked  BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_attempts INT           NOT NULL DEFAULT 0,
    lock_time       DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(500)  NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    expiry_date     DATETIME      NOT NULL,
    revoked         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);

-- Users seeded at runtime by DataInitializer with proper BCrypt (Password@123)
