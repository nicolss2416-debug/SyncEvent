package com.syncevent.domain.model;

public class EventPostponementNotAllowedException extends RuntimeException {

    public EventPostponementNotAllowedException(String message) {
        super(message);
    }
}
