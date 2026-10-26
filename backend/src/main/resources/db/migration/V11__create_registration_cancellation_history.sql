CREATE TABLE registration_cancellation_history (
    id UUID PRIMARY KEY,
    registration_id UUID NOT NULL UNIQUE REFERENCES event_registrations(id),
    canceled_by UUID NOT NULL REFERENCES user_accounts(id),
    reason VARCHAR(2000) NOT NULL,
    canceled_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_registration_cancellation_reason CHECK (length(trim(reason)) > 0)
);

CREATE INDEX idx_registration_cancellation_history_canceled_at
    ON registration_cancellation_history (canceled_at DESC);
