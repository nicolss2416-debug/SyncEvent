package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.AttendanceAbsence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_absences")
public class AttendanceAbsenceEntity {

    @Id
    private UUID id;

    @Column(name = "registration_id", nullable = false, updatable = false)
    private UUID registrationId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    protected AttendanceAbsenceEntity() {
    }

    static AttendanceAbsenceEntity fromDomain(AttendanceAbsence absence) {
        AttendanceAbsenceEntity entity = new AttendanceAbsenceEntity();
        entity.id = absence.id();
        entity.registrationId = absence.registrationId();
        entity.eventId = absence.eventId();
        entity.participantId = absence.participantId();
        entity.recordedAt = absence.recordedAt();
        return entity;
    }

    AttendanceAbsence toDomain() {
        return new AttendanceAbsence(id, registrationId, eventId, participantId, recordedAt);
    }
}
