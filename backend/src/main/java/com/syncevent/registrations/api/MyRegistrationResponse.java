package com.syncevent.registrations.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.registrations.domain.MyRegistration;

public record MyRegistrationResponse(
        UUID id,
        UUID eventId,
        String eventName,
        UUID participantId,
        String status,
        String qrToken,
        LocalDateTime createdAt,
        boolean checkedIn) {

    public static MyRegistrationResponse from(MyRegistration registration) {
        return new MyRegistrationResponse(
                registration.id(),
                registration.eventId(),
                registration.eventName(),
                registration.participantId(),
                registration.status().name(),
                registration.qrToken(),
                registration.createdAt(),
                registration.checkedIn());
    }
}
