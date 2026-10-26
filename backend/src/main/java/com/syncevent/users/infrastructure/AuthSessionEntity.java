package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_sessions")
public class AuthSessionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected AuthSessionEntity() {
    }

    static AuthSessionEntity create(UUID id, UUID userId, LocalDateTime createdAt) {
        AuthSessionEntity entity = new AuthSessionEntity();
        entity.id = id;
        entity.userId = userId;
        entity.createdAt = createdAt;
        entity.lastActivityAt = createdAt;
        return entity;
    }
}
