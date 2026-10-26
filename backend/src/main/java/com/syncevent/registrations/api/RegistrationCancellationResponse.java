package com.syncevent.registrations.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.registrations.application.RegistrationCancellation;

public record RegistrationCancellationResponse(
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        String status,
        UUID canceledBy,
        String reason,
        LocalDateTime canceledAt) {

    static RegistrationCancellationResponse from(RegistrationCancellation cancellation) {
        return new RegistrationCancellationResponse(
                cancellation.registrationId(),
                cancellation.eventId(),
                cancellation.participantId(),
                "CANCELADA",
                cancellation.canceledBy(),
                cancellation.reason(),
                cancellation.canceledAt());
    }
}
