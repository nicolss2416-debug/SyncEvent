package com.syncevent.participants.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.CheckInMethod;
import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.registrations.domain.RegistrationStatus;

public record ParticipantHistoryEntry(
        UUID registrationId,
        UUID eventId,
        String eventName,
        LocalDateTime eventStartAt,
        EstadoEvento eventStatus,
        RegistrationStatus registrationStatus,
        LocalDateTime checkedInAt,
        CheckInMethod checkInMethod,
        LocalDateTime absenceRecordedAt,
        UUID fineId,
        FineStatus fineStatus,
        LocalDateTime fineCreatedAt,
        LocalDateTime fineResolvedAt,
        String cancellationReason) {
}
