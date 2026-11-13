-- =============================================================================
-- V20: Soporte para restablecimiento de contraseñas.
--   - Registra cuándo cambió la contraseña por última vez.
--   - Permite invalidar todas las sesiones activas de una cuenta cuando se
--     restablece la contraseña o se cambia su rol.
-- =============================================================================

ALTER TABLE user_accounts
    ADD COLUMN password_changed_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE user_accounts
SET password_changed_at = created_at
WHERE password_changed_at IS NULL;

ALTER TABLE user_accounts
    ALTER COLUMN password_changed_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE user_accounts
    ALTER COLUMN password_changed_at SET NOT NULL;

-- Acelera la revocación masiva de sesiones de una cuenta.
CREATE INDEX IF NOT EXISTS idx_auth_sessions_user
    ON auth_sessions (user_id);
