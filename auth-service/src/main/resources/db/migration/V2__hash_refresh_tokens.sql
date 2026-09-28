-- Replace plaintext refresh tokens with SHA-256 hashes.
-- Existing rows are migrated before the application switches to hash-based lookups.
UPDATE refresh_tokens
SET token = SHA2(token, 256)
WHERE token IS NOT NULL
  AND CHAR_LENGTH(token) <> 64;

ALTER TABLE refresh_tokens
    MODIFY COLUMN token VARCHAR(64) NOT NULL;
