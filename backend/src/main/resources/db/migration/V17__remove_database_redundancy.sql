-- V17: Eliminación de redundancia de datos (normalización 3FN) y refuerzo de integridad referencial

-- 1. certificate_documents:
--    event_id, participant_id, event_start_at y event_end_at se derivan de
--    registration_id -> event_registrations -> (events, participants).
ALTER TABLE certificate_documents
    ADD CONSTRAINT fk_certificate_documents_registration
        FOREIGN KEY (registration_id) REFERENCES event_registrations(id),
    ADD CONSTRAINT fk_certificate_documents_issued_by
        FOREIGN KEY (issued_by) REFERENCES user_accounts(id),
    ADD CONSTRAINT ck_certificate_documents_status
        CHECK (status IN ('EMITIDO', 'REVOCADO'));

DROP INDEX IF EXISTS idx_certificate_event_id;
DROP INDEX IF EXISTS idx_certificate_participant_id;

ALTER TABLE certificate_documents
    DROP COLUMN event_id,
    DROP COLUMN participant_id,
    DROP COLUMN event_start_at,
    DROP COLUMN event_end_at;

CREATE INDEX idx_certificate_documents_issued_at
    ON certificate_documents (issued_at DESC);

-- 2. attendance_absences:
--    event_id y participant_id se derivan de registration_id -> event_registrations.
DROP INDEX IF EXISTS idx_attendance_absences_participant_recorded;

ALTER TABLE attendance_absences
    DROP COLUMN event_id,
    DROP COLUMN participant_id;

CREATE INDEX idx_attendance_absences_recorded_at
    ON attendance_absences (recorded_at);

-- 3. participant_fines:
--    participant_id se deriva de
--    triggered_by_absence_id -> attendance_absences.registration_id -> event_registrations.participant_id.
DROP INDEX IF EXISTS idx_participant_fines_participant_status;

ALTER TABLE participant_fines
    DROP COLUMN participant_id;
