package com.syncevent.users.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record LinkParticipantRequest(@NotNull UUID participantId) {
}
