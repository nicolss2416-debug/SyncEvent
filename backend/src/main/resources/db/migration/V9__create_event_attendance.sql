CREATE TABLE event_attendance (
    registration_id UUID PRIMARY KEY REFERENCES event_registrations(id),
    checked_in_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    checked_in_by UUID NOT NULL REFERENCES user_accounts(id),
    method VARCHAR(10) NOT NULL,
    CONSTRAINT ck_event_attendance_method CHECK (method IN ('QR', 'MANUAL'))
);
