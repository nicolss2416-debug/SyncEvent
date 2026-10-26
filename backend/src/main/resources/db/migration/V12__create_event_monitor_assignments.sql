CREATE TABLE event_monitor_assignments (
    event_id UUID NOT NULL REFERENCES events(id),
    monitor_id UUID NOT NULL REFERENCES user_accounts(id),
    assigned_by UUID NOT NULL REFERENCES user_accounts(id),
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, monitor_id)
);

CREATE INDEX idx_event_monitor_assignments_monitor
    ON event_monitor_assignments (monitor_id, event_id);
