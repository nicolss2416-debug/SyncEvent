package com.syncevent.registrations.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record MyRegistration(
        UUID id,
        UUID eventId,
        String eventName,
        UUID participantId,
        RegistrationStatus status,
        String qrToken,
        LocalDateTime createdAt,
        boolean checkedIn) {
}
