package com.syncevent.attendance.api;

import java.util.UUID;

import com.syncevent.attendance.application.AttendanceLiveStats;
import com.syncevent.domain.model.EstadoEvento;

public record AttendanceLiveStatsResponse(
        UUID eventId,
        EstadoEvento eventStatus,
        long checkedInCount,
        long activeRegistrationCount,
        int capacity,
        double occupancyPercentage,
        boolean capacityAlert) {

    static AttendanceLiveStatsResponse from(AttendanceLiveStats stats) {
        return new AttendanceLiveStatsResponse(
                stats.eventId(),
                stats.eventStatus(),
                stats.checkedInCount(),
                stats.activeRegistrationCount(),
                stats.capacity(),
                stats.occupancyPercentage(),
                stats.capacityAlert());
    }
}
