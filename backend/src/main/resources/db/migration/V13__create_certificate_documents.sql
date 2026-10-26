CREATE TABLE certificate_documents (
    id UUID PRIMARY KEY,
    registration_id UUID NOT NULL UNIQUE,
    event_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    issued_by UUID NOT NULL,
    verification_code VARCHAR(64) NOT NULL UNIQUE,
    participant_name VARCHAR(255) NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    event_start_at TIMESTAMP NOT NULL,
    event_end_at TIMESTAMP NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    pdf_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL,
    revoked_at TIMESTAMP NULL,
    revocation_reason VARCHAR(255) NULL
);

CREATE INDEX idx_certificate_event_id ON certificate_documents(event_id);
CREATE INDEX idx_certificate_participant_id ON certificate_documents(participant_id);
