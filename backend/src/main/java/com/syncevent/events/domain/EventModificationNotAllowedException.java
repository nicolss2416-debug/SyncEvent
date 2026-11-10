package com.syncevent.events.domain;

public class EventModificationNotAllowedException extends RuntimeException {

    public EventModificationNotAllowedException(String message) {
        super(message);
    }
}
