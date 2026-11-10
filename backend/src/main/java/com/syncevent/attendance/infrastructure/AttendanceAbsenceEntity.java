package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.AttendanceAbsence;
import com.syncevent.registrations.infrastructure.RegistrationEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "attendance_absences")
public class AttendanceAbsenceEntity {

    @Id
    private UUID id;

    @Column(name = "registration_id", nullable = false, unique = true, updatable = false)
    private UUID registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, insertable = false, updatable = false)
    private RegistrationEntity registration;

    @Transient
    private UUID transientEventId;

    @Transient
    private UUID transientParticipantId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    protected AttendanceAbsenceEntity() {
    }

    public RegistrationEntity getRegistration() {
        return registration;
    }

    static AttendanceAbsenceEntity fromDomain(AttendanceAbsence absence) {
        AttendanceAbsenceEntity entity = new AttendanceAbsenceEntity();
        entity.id = absence.id();
        entity.registrationId = absence.registrationId();
        entity.transientEventId = absence.eventId();
        entity.transientParticipantId = absence.participantId();
        entity.recordedAt = absence.recordedAt();
        return entity;
    }

    AttendanceAbsence toDomain() {
        UUID resolvedEventId = registration != null && registration.getEvent() != null
                ? registration.getEvent().getId()
                : transientEventId;
        UUID resolvedParticipantId = registration != null && registration.getParticipant() != null
                ? registration.getParticipant().getId()
                : transientParticipantId;
        return new AttendanceAbsence(id, registrationId, resolvedEventId, resolvedParticipantId, recordedAt);
    }
}
