package com.syncevent.attendance.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record FinePolicy(
        int absenceThreshold,
        int windowDays,
        UUID updatedBy,
        LocalDateTime updatedAt) {

    public FinePolicy {
        if (absenceThreshold <= 0 || windowDays <= 0) {
            throw new IllegalArgumentException("El umbral y el período deben ser mayores que cero.");
        }
    }
}
