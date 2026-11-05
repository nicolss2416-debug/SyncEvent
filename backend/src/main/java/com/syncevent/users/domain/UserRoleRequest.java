package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRoleRequest(
        UUID id,
        UUID userId,
        UserRole requestedRole,
        RoleRequestStatus status,
        LocalDateTime requestedAt,
        UUID resolvedBy,
        LocalDateTime resolvedAt) {

    public static UserRoleRequest pending(UUID userId, UserRole role, LocalDateTime requestedAt) {
        return new UserRoleRequest(
                UUID.randomUUID(), userId, role, RoleRequestStatus.PENDIENTE, requestedAt, null, null);
    }

    public UserRoleRequest resolve(boolean approved, UUID administratorId, LocalDateTime resolvedAt) {
        if (status != RoleRequestStatus.PENDIENTE) {
            throw new IllegalStateException("La solicitud de rol ya fue resuelta.");
        }
        return new UserRoleRequest(
                id,
                userId,
                requestedRole,
                approved ? RoleRequestStatus.APROBADA : RoleRequestStatus.RECHAZADA,
                requestedAt,
                administratorId,
                resolvedAt);
    }
}
