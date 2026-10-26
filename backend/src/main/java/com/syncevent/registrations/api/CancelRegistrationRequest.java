package com.syncevent.registrations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRegistrationRequest(
        @NotBlank @Size(max = 2000) String reason) {
}
