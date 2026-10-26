package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.attendance.domain.ParticipantFine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "participant_fines")
public class ParticipantFineEntity {

    @Id
    private UUID id;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "triggered_by_absence_id", nullable = false, updatable = false)
    private UUID triggeredByAbsenceId;

    @Column(name = "absence_threshold", nullable = false, updatable = false)
    private int absenceThreshold;

    @Column(name = "window_days", nullable = false, updatable = false)
    private int windowDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected ParticipantFineEntity() {
    }

    static ParticipantFineEntity fromDomain(ParticipantFine fine) {
        ParticipantFineEntity entity = new ParticipantFineEntity();
        entity.id = fine.id();
        entity.participantId = fine.participantId();
        entity.triggeredByAbsenceId = fine.triggeredByAbsenceId();
        entity.absenceThreshold = fine.absenceThreshold();
        entity.windowDays = fine.windowDays();
        entity.status = fine.status();
        entity.createdAt = fine.createdAt();
        entity.resolvedBy = fine.resolvedBy();
        entity.resolvedAt = fine.resolvedAt();
        return entity;
    }

    ParticipantFine toDomain() {
        return new ParticipantFine(
                id,
                participantId,
                triggeredByAbsenceId,
                absenceThreshold,
                windowDays,
                status,
                createdAt,
                resolvedBy,
                resolvedAt);
    }
}
