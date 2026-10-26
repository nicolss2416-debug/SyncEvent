package com.syncevent.attendance.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ManualCheckInRequest(@NotNull UUID participantId) {
}
