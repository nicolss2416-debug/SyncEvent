package com.syncevent.participants.application;

public record UpdateParticipantCommand(
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String updatedBy) {
}
