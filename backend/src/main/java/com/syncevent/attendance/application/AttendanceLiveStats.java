package com.syncevent.attendance.application;

import java.util.UUID;

import com.syncevent.domain.model.EstadoEvento;

public record AttendanceLiveStats(
        UUID eventId,
        EstadoEvento eventStatus,
        long checkedInCount,
        long activeRegistrationCount,
        int capacity,
        double occupancyPercentage,
        boolean capacityAlert) {
}
