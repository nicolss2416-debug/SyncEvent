package com.syncevent.registrations.domain;

import java.util.UUID;

public class RegistrationNotFoundException extends RuntimeException {

    public RegistrationNotFoundException(UUID registrationId) {
        super("No existe una inscripción con identificador %s.".formatted(registrationId));
    }
}
