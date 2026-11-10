-- =============================================================================
-- V19: Integridad de base de datos, alineación de estados/enums, índices de
--      rendimiento y limpieza de tablas sin uso.
-- =============================================================================

-- 1. Alinear ck_certificate_documents_status con CertificateStatus (VIGENTE / REVOCADO)
ALTER TABLE certificate_documents DROP CONSTRAINT IF EXISTS ck_certificate_documents_status;
UPDATE certificate_documents SET status = 'VIGENTE' WHERE status = 'EMITIDO';
ALTER TABLE certificate_documents
    ADD CONSTRAINT ck_certificate_documents_status
    CHECK (status IN ('VIGENTE', 'REVOCADO'));

-- 2. Restricciones CHECK en tabla events (type, modality, status)
ALTER TABLE events DROP CONSTRAINT IF EXISTS ck_events_type;
ALTER TABLE events
    ADD CONSTRAINT ck_events_type
    CHECK (type IN ('CAPACITACION', 'TALLER', 'TORNEO', 'CONFERENCIA', 'SEMINARIO'));

ALTER TABLE events DROP CONSTRAINT IF EXISTS ck_events_modality;
ALTER TABLE events
    ADD CONSTRAINT ck_events_modality
    CHECK (modality IN ('PRESENCIAL', 'VIRTUAL', 'HIBRIDA', 'HIBRIDO'));

ALTER TABLE events DROP CONSTRAINT IF EXISTS ck_events_status;
ALTER TABLE events
    ADD CONSTRAINT ck_events_status
    CHECK (status IN ('BORRADOR', 'PUBLICADO', 'APLAZADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'));

-- 3. Restricciones CHECK en tablas de historial y auditoría
ALTER TABLE event_cancellation_history DROP CONSTRAINT IF EXISTS ck_event_cancellation_history_reason;
ALTER TABLE event_cancellation_history
    ADD CONSTRAINT ck_event_cancellation_history_reason
    CHECK (length(trim(reason)) > 0);

ALTER TABLE event_postponement_history DROP CONSTRAINT IF EXISTS ck_event_postponement_history_action;
ALTER TABLE event_postponement_history
    ADD CONSTRAINT ck_event_postponement_history_action
    CHECK (action IN ('SOLICITADO', 'CONFIRMADO'));

ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS ck_audit_log_result;
ALTER TABLE audit_log
    ADD CONSTRAINT ck_audit_log_result
    CHECK (result IN ('EXITO', 'FALLO'));

-- 4. Índices compuestos de rendimiento para consultas frecuentes
CREATE INDEX IF NOT EXISTS idx_event_registrations_event_status
    ON event_registrations (event_id, status);

CREATE INDEX IF NOT EXISTS idx_user_accounts_active
    ON user_accounts (active);

CREATE INDEX IF NOT EXISTS idx_participants_email
    ON participants (email);

CREATE INDEX IF NOT EXISTS idx_participants_names
    ON participants (first_name, last_name);

-- 5. Eliminar tabla huérfana user_role_requests (reemplazada por rol único directo)
DROP TABLE IF EXISTS user_role_requests;
