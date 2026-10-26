package com.syncevent.events.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignMonitorRequest(@NotNull UUID monitorId) {
}
