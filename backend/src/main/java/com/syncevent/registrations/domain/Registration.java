package com.syncevent.registrations.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Registration {

    private final UUID id;
    private final UUID eventId;
    private final UUID participantId;
    private final String qrToken;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private final RegistrationStatus status;

    private Registration(
            UUID id,
            UUID eventId,
            UUID participantId,
            String qrToken,
            UUID createdBy,
            LocalDateTime createdAt,
            RegistrationStatus status) {
        this.id = id;
        this.eventId = eventId;
        this.participantId = participantId;
        this.qrToken = qrToken;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = status;
    }

    public static Registration create(
            UUID eventId,
            UUID participantId,
            String qrToken,
            UUID createdBy,
            LocalDateTime createdAt) {
        return new Registration(
                UUID.randomUUID(),
                eventId,
                participantId,
                qrToken,
                createdBy,
                createdAt,
                RegistrationStatus.ACTIVA);
    }

    public static Registration reconstitute(
            UUID id,
            UUID eventId,
            UUID participantId,
            String qrToken,
            UUID createdBy,
            LocalDateTime createdAt,
            RegistrationStatus status) {
        return new Registration(id, eventId, participantId, qrToken, createdBy, createdAt, status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public RegistrationStatus getStatus() {
        return status;
    }
}
