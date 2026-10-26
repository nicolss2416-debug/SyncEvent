CREATE TABLE event_registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    participant_id UUID NOT NULL REFERENCES participants(id),
    qr_token VARCHAR(64) NOT NULL UNIQUE,
    created_by UUID NOT NULL REFERENCES user_accounts(id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_event_registrations_status CHECK (status IN ('ACTIVA')),
    CONSTRAINT uk_event_registrations_event_participant UNIQUE (event_id, participant_id)
);

CREATE INDEX idx_event_registrations_participant_status
    ON event_registrations (participant_id, status);
