package com.syncevent.events.domain;

import java.util.UUID;

public record AssignedMonitor(UUID eventId, String email) {
}
