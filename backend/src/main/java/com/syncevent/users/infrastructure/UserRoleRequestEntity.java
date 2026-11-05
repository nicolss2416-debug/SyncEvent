package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.users.domain.RoleRequestStatus;
import com.syncevent.users.domain.UserRole;
import com.syncevent.users.domain.UserRoleRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_role_requests")
public class UserRoleRequestEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 30, updatable = false)
    private UserRole requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleRequestStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected UserRoleRequestEntity() {
    }

    static UserRoleRequestEntity fromDomain(UserRoleRequest request) {
        UserRoleRequestEntity entity = new UserRoleRequestEntity();
        entity.id = request.id();
        entity.userId = request.userId();
        entity.requestedRole = request.requestedRole();
        entity.status = request.status();
        entity.requestedAt = request.requestedAt();
        entity.resolvedBy = request.resolvedBy();
        entity.resolvedAt = request.resolvedAt();
        return entity;
    }

    UserRoleRequest toDomain() {
        return new UserRoleRequest(id, userId, requestedRole, status, requestedAt, resolvedBy, resolvedAt);
    }
}
