package com.syncevent.registrations.application;

public class RegistrationOwnershipException extends RuntimeException {

    public RegistrationOwnershipException() {
        super("El Invitado solo puede cancelar sus propias inscripciones.");
    }
}
