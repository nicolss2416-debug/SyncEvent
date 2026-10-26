package com.syncevent.registrations.application;

public class DuplicateRegistrationException extends RuntimeException {

    public DuplicateRegistrationException() {
        super("El participante ya tiene una inscripción activa para este evento.");
    }
}
