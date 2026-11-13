package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.tokenHash = :tokenHash AND t.used = false AND t.expiresAt > :now")
    Optional<PasswordResetTokenEntity> findActiveByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetTokenEntity t SET t.used = true WHERE t.userId = :userId AND t.used = false")
    int invalidateAllForUser(@Param("userId") UUID userId);
}
