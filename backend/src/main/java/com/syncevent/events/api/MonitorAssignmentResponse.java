package com.syncevent.events.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.events.domain.model.MonitorAssignment;

public record MonitorAssignmentResponse(
        UUID eventId,
        UUID monitorId,
        UUID assignedBy,
        LocalDateTime assignedAt) {

    static MonitorAssignmentResponse from(MonitorAssignment assignment) {
        return new MonitorAssignmentResponse(
                assignment.eventId(),
                assignment.monitorId(),
                assignment.assignedBy(),
                assignment.assignedAt());
    }
}
