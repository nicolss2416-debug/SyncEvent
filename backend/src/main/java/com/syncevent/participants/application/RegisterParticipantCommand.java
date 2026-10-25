package com.syncevent.participants.application;

public record RegisterParticipantCommand(
        String documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String email,
        String phone,
        String createdBy) {
}
