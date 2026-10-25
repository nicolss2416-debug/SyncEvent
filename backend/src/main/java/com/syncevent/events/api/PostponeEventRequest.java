package com.syncevent.events.api;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostponeEventRequest(
        @NotBlank @Size(max = 2000) String reason,
        @NotNull LocalDateTime newStartAt,
        @NotNull LocalDateTime newEndAt) {
}
