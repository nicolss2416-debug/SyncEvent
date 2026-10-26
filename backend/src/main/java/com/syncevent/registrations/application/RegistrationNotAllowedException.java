package com.syncevent.registrations.application;

public class RegistrationNotAllowedException extends RuntimeException {

    public RegistrationNotAllowedException(String message) {
        super(message);
    }
}
