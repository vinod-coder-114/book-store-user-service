-- Schema bootstrap for user-service.
-- Mounted into MySQL's /docker-entrypoint-initdb.d so it runs automatically
-- on first container start (only when the data volume is empty).

CREATE TABLE IF NOT EXISTS revoked_tokens (
    jti         VARCHAR(255) NOT NULL PRIMARY KEY,
    expires_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NOT NULL
);

