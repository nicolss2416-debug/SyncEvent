package com.syncevent.participants.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.participants.domain.ParticipantHistoryEntry;

public record ParticipantHistoryEntryResponse(
        UUID registrationId,
        UUID eventId,
        String eventName,
        LocalDateTime eventStartAt,
        String eventStatus,
        String registrationStatus,
        LocalDateTime checkedInAt,
        String checkInMethod,
        LocalDateTime absenceRecordedAt,
        UUID fineId,
        String fineStatus,
        LocalDateTime fineCreatedAt,
        LocalDateTime fineResolvedAt,
        String cancellationReason) {

    static ParticipantHistoryEntryResponse from(ParticipantHistoryEntry entry) {
        return new ParticipantHistoryEntryResponse(
                entry.registrationId(),
                entry.eventId(),
                entry.eventName(),
                entry.eventStartAt(),
                entry.eventStatus().name(),
                entry.registrationStatus().name(),
                entry.checkedInAt(),
                entry.checkInMethod() == null ? null : entry.checkInMethod().name(),
                entry.absenceRecordedAt(),
                entry.fineId(),
                entry.fineStatus() == null ? null : entry.fineStatus().name(),
                entry.fineCreatedAt(),
                entry.fineResolvedAt(),
                entry.cancellationReason());
    }
}
