package com.syncevent.attendance.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.application.AttendanceCheckIn;

public record AttendanceResponse(
        UUID eventId,
        UUID registrationId,
        UUID participantId,
        String participantName,
        String documentType,
        String documentNumber,
        String method,
        LocalDateTime checkedInAt,
        UUID checkedInBy) {

    static AttendanceResponse from(AttendanceCheckIn result) {
        return new AttendanceResponse(
                result.registration().eventId(),
                result.registration().registrationId(),
                result.registration().participantId(),
                result.registration().participantName(),
                result.registration().documentType(),
                result.registration().documentNumber(),
                result.checkIn().method().name(),
                result.checkIn().checkedInAt(),
                result.checkIn().checkedInBy());
    }
}
