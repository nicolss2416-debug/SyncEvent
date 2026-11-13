package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPersistencePort {

    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, LocalDateTime now);

    void invalidateAllForUser(UUID userId, LocalDateTime now);
}
