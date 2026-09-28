-- Repair the refresh-token schema for databases that were partially initialized
-- before all authentication migrations were applied.
--
-- The table definition is intentionally idempotent. Do not create indexes
-- separately here: InnoDB creates the required foreign-key index when the
-- table is created, and V1/V3 may already have created these indexes in
-- partially initialized databases.

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(64)   NOT NULL UNIQUE,
    user_id         BIGINT        NOT NULL,
    expiry_date     DATETIME      NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id)
);