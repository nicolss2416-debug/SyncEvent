package com.syncevent.registrations.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.AttendanceCandidate;
import com.syncevent.attendance.domain.AttendanceRegistration;
import com.syncevent.domain.model.Event;
import com.syncevent.events.infrastructure.persistence.EventEntity;
import com.syncevent.participants.infrastructure.ParticipantEntity;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "event_registrations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_event_registrations_event_participant",
                columnNames = { "event_id", "participant_id" }),
        @UniqueConstraint(name = "uk_event_registrations_qr_token", columnNames = "qr_token")
})
public class RegistrationEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false, updatable = false)
    private ParticipantEntity participant;

    @Column(name = "qr_token", nullable = false, length = 64, updatable = false)
    private String qrToken;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    protected RegistrationEntity() {
    }

    static RegistrationEntity fromDomain(Registration registration) {
        RegistrationEntity entity = new RegistrationEntity();
        entity.id = registration.getId();
        entity.event = EventEntity.reference(registration.getEventId());
        entity.participant = ParticipantEntity.reference(registration.getParticipantId());
        entity.qrToken = registration.getQrToken();
        entity.createdBy = registration.getCreatedBy();
        entity.createdAt = registration.getCreatedAt();
        entity.status = registration.getStatus();
        return entity;
    }

    Registration toDomain() {
        return Registration.reconstitute(
                id, event.getId(), participant.getId(), qrToken, createdBy, createdAt, status);
    }

    Event eventToDomain() {
        return event.toDomain();
    }

    AttendanceRegistration toAttendanceRegistration() {
        var participantDetails = participant.toDomain();
        return new AttendanceRegistration(
                id,
                event.getId(),
                participant.getId(),
                qrToken,
                status,
                event.getStatus(),
                participantDetails.getDocumentType(),
                participantDetails.getDocumentNumber(),
                participantDetails.getFirstName(),
                participantDetails.getLastName());
    }

    AttendanceCandidate toAttendanceCandidate() {
        AttendanceRegistration details = toAttendanceRegistration();
        return new AttendanceCandidate(
                details.registrationId(),
                details.participantId(),
                details.documentType(),
                details.documentNumber(),
                details.firstName(),
                details.lastName());
    }
}
