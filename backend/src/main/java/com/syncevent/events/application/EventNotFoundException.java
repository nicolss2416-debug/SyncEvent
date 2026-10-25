package com.syncevent.events.application;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(UUID eventId) {
        super("No existe un evento con identificador %s.".formatted(eventId));
    }
}
