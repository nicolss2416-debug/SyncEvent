package com.syncevent.attendance.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.ParticipantFine;

public record ParticipantFineResponse(
        UUID id,
        UUID participantId,
        UUID triggeredByAbsenceId,
        int absenceThreshold,
        int windowDays,
        String status,
        LocalDateTime createdAt,
        UUID resolvedBy,
        LocalDateTime resolvedAt) {

    static ParticipantFineResponse from(ParticipantFine fine) {
        return new ParticipantFineResponse(
                fine.id(),
                fine.participantId(),
                fine.triggeredByAbsenceId(),
                fine.absenceThreshold(),
                fine.windowDays(),
                fine.status().name(),
                fine.createdAt(),
                fine.resolvedBy(),
                fine.resolvedAt());
    }
}
