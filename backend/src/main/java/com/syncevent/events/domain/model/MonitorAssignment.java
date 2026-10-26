package com.syncevent.events.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonitorAssignment(
        UUID eventId,
        UUID monitorId,
        UUID assignedBy,
        LocalDateTime assignedAt) {
}
