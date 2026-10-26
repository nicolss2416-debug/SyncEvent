package com.syncevent.certificates.api;

import jakarta.validation.constraints.NotBlank;

public record RevokeCertificateRequest(@NotBlank(message = "El motivo es obligatorio") String reason) {
}
