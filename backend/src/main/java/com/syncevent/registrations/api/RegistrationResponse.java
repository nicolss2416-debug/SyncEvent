package com.syncevent.registrations.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.registrations.domain.Registration;

public record RegistrationResponse(
        UUID id,
        UUID eventId,
        UUID participantId,
        String status,
        String qrToken,
        LocalDateTime createdAt) {

    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getEventId(),
                registration.getParticipantId(),
                registration.getStatus().name(),
                registration.getQrToken(),
                registration.getCreatedAt());
    }
}
