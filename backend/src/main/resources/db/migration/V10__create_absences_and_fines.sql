ALTER TABLE event_registrations
    DROP CONSTRAINT ck_event_registrations_status;

ALTER TABLE event_registrations
    ADD CONSTRAINT ck_event_registrations_status CHECK (status IN ('ACTIVA', 'CANCELADA'));

CREATE TABLE attendance_policy (
    id INTEGER PRIMARY KEY,
    absence_threshold INTEGER NOT NULL,
    window_days INTEGER NOT NULL,
    updated_by UUID REFERENCES user_accounts(id),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_attendance_policy_singleton CHECK (id = 1),
    CONSTRAINT ck_attendance_policy_threshold CHECK (absence_threshold > 0),
    CONSTRAINT ck_attendance_policy_window CHECK (window_days > 0)
);

INSERT INTO attendance_policy (id, absence_threshold, window_days, updated_at)
VALUES (1, 3, 90, CURRENT_TIMESTAMP);

CREATE TABLE attendance_absences (
    id UUID PRIMARY KEY,
    registration_id UUID NOT NULL UNIQUE REFERENCES event_registrations(id),
    event_id UUID NOT NULL REFERENCES events(id),
    participant_id UUID NOT NULL REFERENCES participants(id),
    recorded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_attendance_absences_participant_recorded
    ON attendance_absences (participant_id, recorded_at);

CREATE TABLE participant_fines (
    id UUID PRIMARY KEY,
    participant_id UUID NOT NULL REFERENCES participants(id),
    triggered_by_absence_id UUID NOT NULL UNIQUE REFERENCES attendance_absences(id),
    absence_threshold INTEGER NOT NULL,
    window_days INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolved_by UUID REFERENCES user_accounts(id),
    resolved_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT ck_participant_fines_status CHECK (status IN ('PENDIENTE', 'RESUELTA')),
    CONSTRAINT ck_participant_fines_resolution CHECK (
        (status = 'PENDIENTE' AND resolved_by IS NULL AND resolved_at IS NULL)
        OR (status = 'RESUELTA' AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
    ),
    CONSTRAINT ck_participant_fines_policy CHECK (absence_threshold > 0 AND window_days > 0)
);

CREATE INDEX idx_participant_fines_participant_status
    ON participant_fines (participant_id, status);

CREATE INDEX idx_participant_fines_status_created
    ON participant_fines (status, created_at DESC);
