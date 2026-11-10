package com.syncevent.events.domain;

public class EventCancellationNotAllowedException extends RuntimeException {

    public EventCancellationNotAllowedException(String message) {
        super(message);
    }
}
