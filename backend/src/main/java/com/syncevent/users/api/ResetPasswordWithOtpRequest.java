package com.syncevent.users.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordWithOtpRequest(
        @NotBlank(message = "El correo electrónico es requerido.")
        @Email(message = "El formato del correo electrónico no es válido.")
        String email,

        @NotBlank(message = "El código de verificación es requerido.")
        @Pattern(regexp = "^[0-9]{6}$", message = "El código de verificación debe contener exactamente 6 dígitos numéricos.")
        String otp,

        @NotBlank(message = "La nueva contraseña es requerida.")
        @Size(min = 8, max = 128, message = "La nueva contraseña debe tener entre 8 y 128 caracteres.")
        String newPassword) {
}
