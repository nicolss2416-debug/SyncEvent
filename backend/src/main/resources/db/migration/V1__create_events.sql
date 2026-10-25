CREATE TABLE events (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    modality VARCHAR(20) NOT NULL,
    start_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    location VARCHAR(250),
    meeting_url VARCHAR(2048),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    duration_hours NUMERIC(8, 2) NOT NULL CHECK (duration_hours > 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CHECK (end_at > start_at)
);

CREATE INDEX idx_events_location_schedule ON events (location, start_at, end_at);
CREATE INDEX idx_events_status ON events (status);