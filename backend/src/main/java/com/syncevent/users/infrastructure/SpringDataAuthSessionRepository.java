package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    @Modifying
    @Query("""
            UPDATE AuthSessionEntity s
            SET s.lastActivityAt = :activityAt
            WHERE s.id = :sessionId
              AND s.revokedAt IS NULL
              AND s.lastActivityAt > :cutoff
              AND EXISTS (
                  SELECT u.id FROM UserAccountEntity u
                  WHERE u.id = s.userId AND u.active = true
              )
            """)
    int updateActivityIfValid(
            @Param("sessionId") UUID sessionId,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("activityAt") LocalDateTime activityAt);

    @Modifying
    @Query("""
            UPDATE AuthSessionEntity s
            SET s.revokedAt = :revokedAt
            WHERE s.id = :sessionId
              AND s.revokedAt IS NULL
            """)
    int revoke(@Param("sessionId") UUID sessionId, @Param("revokedAt") LocalDateTime revokedAt);
}
