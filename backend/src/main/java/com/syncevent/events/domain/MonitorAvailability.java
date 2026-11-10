package com.syncevent.events.domain;

import java.util.UUID;

public record MonitorAvailability(
        UUID monitorId,
        String email,
        boolean active,
        boolean assignedToEvent,
        UUID conflictingEventId,
        String conflictingEventName) {

    public boolean available() {
        return active && !assignedToEvent && conflictingEventId == null;
    }
}
