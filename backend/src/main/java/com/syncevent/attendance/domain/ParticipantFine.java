package com.syncevent.attendance.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ParticipantFine(
        UUID id,
        UUID participantId,
        UUID triggeredByAbsenceId,
        int absenceThreshold,
        int windowDays,
        FineStatus status,
        LocalDateTime createdAt,
        UUID resolvedBy,
        LocalDateTime resolvedAt) {

    public ParticipantFine resolve(UUID administratorId, LocalDateTime resolvedAt) {
        if (status != FineStatus.PENDIENTE) {
            throw new FineAlreadyResolvedException(id);
        }
        return new ParticipantFine(
                id,
                participantId,
                triggeredByAbsenceId,
                absenceThreshold,
                windowDays,
                FineStatus.RESUELTA,
                createdAt,
                administratorId,
                resolvedAt);
    }
}
