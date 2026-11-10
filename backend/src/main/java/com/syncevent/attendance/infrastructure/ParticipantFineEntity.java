package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.attendance.domain.ParticipantFine;
import com.syncevent.users.infrastructure.UserAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "participant_fines")
public class ParticipantFineEntity {

    @Id
    private UUID id;

    @Transient
    private UUID transientParticipantId;

    @Column(name = "triggered_by_absence_id", nullable = false, unique = true, updatable = false)
    private UUID triggeredByAbsenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by_absence_id", nullable = false, insertable = false, updatable = false)
    private AttendanceAbsenceEntity triggeredByAbsence;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by", insertable = false, updatable = false)
    private UserAccountEntity resolvedByUser;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected ParticipantFineEntity() {
    }

    static ParticipantFineEntity fromDomain(ParticipantFine fine) {
        ParticipantFineEntity entity = new ParticipantFineEntity();
        entity.id = fine.id();
        entity.transientParticipantId = fine.participantId();
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
        UUID resolvedParticipantId = triggeredByAbsence != null
                && triggeredByAbsence.getRegistration() != null
                && triggeredByAbsence.getRegistration().getParticipant() != null
                        ? triggeredByAbsence.getRegistration().getParticipant().getId()
                        : transientParticipantId;
        return new ParticipantFine(
                id,
                resolvedParticipantId,
                triggeredByAbsenceId,
                absenceThreshold,
                windowDays,
                status,
                createdAt,
                resolvedBy,
                resolvedAt);
    }
}
