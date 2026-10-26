package com.syncevent.registrations.application;

public class PendingFineRegistrationException extends RuntimeException {

    public PendingFineRegistrationException() {
        super("El participante tiene una multa pendiente de resolución administrativa.");
    }
}
