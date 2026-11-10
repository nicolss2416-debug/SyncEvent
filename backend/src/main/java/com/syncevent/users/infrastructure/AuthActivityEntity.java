package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_activity")
public class AuthActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccountEntity user;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 30)
    private String activity;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected AuthActivityEntity() {
    }

    static AuthActivityEntity create(UUID userId, String email, String activity, LocalDateTime occurredAt) {
        AuthActivityEntity entity = new AuthActivityEntity();
        entity.userId = userId;
        entity.email = email;
        entity.activity = activity;
        entity.occurredAt = occurredAt;
        return entity;
    }
}
