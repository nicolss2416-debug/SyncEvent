package com.syncevent.events.domain;

public class EventPostponementNotAllowedException extends RuntimeException {

    public EventPostponementNotAllowedException(String message) {
        super(message);
    }
}
