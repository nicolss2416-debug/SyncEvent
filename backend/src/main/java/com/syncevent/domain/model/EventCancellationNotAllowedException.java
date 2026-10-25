package com.syncevent.domain.model;

public class EventCancellationNotAllowedException extends RuntimeException {

    public EventCancellationNotAllowedException(String message) {
        super(message);
    }
}
