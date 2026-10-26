package com.syncevent.attendance.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckIn(
        UUID registrationId,
        LocalDateTime checkedInAt,
        UUID checkedInBy,
        CheckInMethod method) {
}
