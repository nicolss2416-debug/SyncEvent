package com.syncevent.events.api;

import java.util.UUID;

import com.syncevent.events.domain.model.MonitorAvailability;

public record MonitorAvailabilityResponse(
        UUID monitorId,
        String email,
        boolean active,
        boolean assignedToEvent,
        boolean available,
        UUID conflictingEventId,
        String conflictingEventName) {

    static MonitorAvailabilityResponse from(MonitorAvailability availability) {
        return new MonitorAvailabilityResponse(
                availability.monitorId(),
                availability.email(),
                availability.active(),
                availability.assignedToEvent(),
                availability.available(),
                availability.conflictingEventId(),
                availability.conflictingEventName());
    }
}
