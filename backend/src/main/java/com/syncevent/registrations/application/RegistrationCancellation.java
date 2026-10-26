package com.syncevent.registrations.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistrationCancellation(
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        UUID canceledBy,
        String reason,
        LocalDateTime canceledAt) {
}
