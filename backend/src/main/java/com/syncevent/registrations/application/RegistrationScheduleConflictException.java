package com.syncevent.registrations.application;

import java.util.UUID;

public class RegistrationScheduleConflictException extends RuntimeException {

    private final UUID conflictingEventId;
    private final String conflictingEventName;

    public RegistrationScheduleConflictException(UUID conflictingEventId, String conflictingEventName) {
        super("El horario se cruza con el evento '%s' (%s).".formatted(conflictingEventName, conflictingEventId));
        this.conflictingEventId = conflictingEventId;
        this.conflictingEventName = conflictingEventName;
    }

    public UUID getConflictingEventId() {
        return conflictingEventId;
    }

    public String getConflictingEventName() {
        return conflictingEventName;
    }
}
