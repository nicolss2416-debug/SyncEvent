package com.syncevent.domain.model;

public class EventModificationNotAllowedException extends RuntimeException {

    public EventModificationNotAllowedException(String message) {
        super(message);
    }
}
