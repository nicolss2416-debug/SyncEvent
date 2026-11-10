package com.syncevent.events.domain;

public class InvalidEventStateTransitionException extends RuntimeException {

    public InvalidEventStateTransitionException(String message) {
        super(message);
    }
}
