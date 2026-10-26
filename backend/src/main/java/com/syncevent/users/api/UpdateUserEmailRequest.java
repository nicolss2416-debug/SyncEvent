package com.syncevent.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserEmailRequest(
        @NotBlank @Email @Size(max = 254) String email) {
}
