package com.syncevent.events.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class EventMonitorAssignmentId implements Serializable {

    private UUID eventId;
    private UUID monitorId;

    public EventMonitorAssignmentId() {
    }

    public EventMonitorAssignmentId(UUID eventId, UUID monitorId) {
        this.eventId = eventId;
        this.monitorId = monitorId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EventMonitorAssignmentId that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId) && Objects.equals(monitorId, that.monitorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, monitorId);
    }
}
