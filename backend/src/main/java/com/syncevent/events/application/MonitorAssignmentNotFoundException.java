package com.syncevent.events.application;

import java.util.UUID;

public class MonitorAssignmentNotFoundException extends RuntimeException {

    public MonitorAssignmentNotFoundException(UUID eventId, UUID monitorId) {
        super("El monitor %s no está asignado al evento %s.".formatted(monitorId, eventId));
    }
}
