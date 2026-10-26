package com.syncevent.registrations.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateRegistrationRequest(@NotNull UUID participantId) {
}
