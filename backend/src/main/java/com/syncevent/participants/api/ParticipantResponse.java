package com.syncevent.participants.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.participants.domain.Participant;

public record ParticipantResponse(
        UUID id,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt) {

    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getDocumentType(),
                participant.getDocumentNumber(),
                participant.getFirstName(),
                participant.getLastName(),
                participant.getEmail(),
                participant.getPhone(),
                participant.getCreatedBy(),
                participant.getCreatedAt(),
                participant.getUpdatedBy(),
                participant.getUpdatedAt());
    }
}
