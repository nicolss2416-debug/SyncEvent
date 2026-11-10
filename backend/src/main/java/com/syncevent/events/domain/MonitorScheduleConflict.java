package com.syncevent.events.domain;

import java.util.UUID;

public record MonitorScheduleConflict(
        UUID monitorId,
        UUID eventId,
        String eventName) {
}
