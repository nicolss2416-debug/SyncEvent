package com.syncevent.participants.application;

import java.util.UUID;

public class DuplicateParticipantException extends RuntimeException {

    private final UUID existingParticipantId;

    public DuplicateParticipantException(UUID existingParticipantId) {
        super("Ya existe un participante con ese tipo y número de documento.");
        this.existingParticipantId = existingParticipantId;
    }

    public UUID getExistingParticipantId() {
        return existingParticipantId;
    }
}
