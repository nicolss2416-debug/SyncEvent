package com.syncevent.events.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelEventRequest(
        @NotBlank @Size(max = 2000) String reason) {
}
