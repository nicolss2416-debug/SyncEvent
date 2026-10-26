package com.syncevent.events.application;

import java.util.UUID;

public class MonitorScheduleConflictException extends RuntimeException {

    private final UUID monitorId;
    private final UUID conflictingEventId;
    private final String conflictingEventName;

    public MonitorScheduleConflictException(
            UUID monitorId, UUID conflictingEventId, String conflictingEventName) {
        super("El monitor ya está asignado al evento '%s' en un horario que se cruza."
                .formatted(conflictingEventName));
        this.monitorId = monitorId;
        this.conflictingEventId = conflictingEventId;
        this.conflictingEventName = conflictingEventName;
    }

    public UUID getMonitorId() {
        return monitorId;
    }

    public UUID getConflictingEventId() {
        return conflictingEventId;
    }

    public String getConflictingEventName() {
        return conflictingEventName;
    }
}
