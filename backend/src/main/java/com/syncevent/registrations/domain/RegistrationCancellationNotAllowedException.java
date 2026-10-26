package com.syncevent.registrations.domain;

public class RegistrationCancellationNotAllowedException extends RuntimeException {

    public RegistrationCancellationNotAllowedException(String message) {
        super(message);
    }
}
