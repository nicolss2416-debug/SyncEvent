package com.syncevent.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "El correo electrónico es requerido.")
        @Email(message = "El formato del correo electrónico no es válido.")
        String email) {
}
