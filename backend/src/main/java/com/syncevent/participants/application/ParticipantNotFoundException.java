package com.syncevent.participants.application;

import java.util.UUID;

public class ParticipantNotFoundException extends RuntimeException {

    public ParticipantNotFoundException(UUID id) {
        super("No se encontró el participante %s.".formatted(id));
    }
}
