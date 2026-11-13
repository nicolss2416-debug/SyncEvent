-- =============================================================================
-- V22: Soporte para restablecimiento autónomo de contraseña mediante OTP.
-- =============================================================================

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens (user_id, used);

CREATE INDEX idx_password_reset_tokens_lookup
    ON password_reset_tokens (token_hash, used, expires_at);
