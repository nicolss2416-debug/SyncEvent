package com.syncevent.domain.model;

public class InvalidEventStateTransitionException extends RuntimeException {

    public InvalidEventStateTransitionException(String message) {
        super(message);
    }
}
