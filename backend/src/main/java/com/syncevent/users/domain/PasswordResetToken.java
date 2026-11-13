package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record PasswordResetToken(
        UUID id,
        UUID userId,
        String tokenHash,
        LocalDateTime expiresAt,
        boolean used,
        LocalDateTime createdAt) {

    public boolean isValid(LocalDateTime now) {
        return !used && expiresAt.isAfter(now);
    }
}
