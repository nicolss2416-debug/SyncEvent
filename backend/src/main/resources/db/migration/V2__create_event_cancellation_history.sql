CREATE TABLE event_cancellation_history (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    previous_status VARCHAR(20) NOT NULL,
    reason VARCHAR(2000) NOT NULL,
    canceled_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_event_cancellation_history_event_id
    ON event_cancellation_history (event_id);
