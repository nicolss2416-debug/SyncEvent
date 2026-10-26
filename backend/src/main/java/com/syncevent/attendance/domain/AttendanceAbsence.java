package com.syncevent.attendance.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceAbsence(
        UUID id,
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        LocalDateTime recordedAt) {
}
