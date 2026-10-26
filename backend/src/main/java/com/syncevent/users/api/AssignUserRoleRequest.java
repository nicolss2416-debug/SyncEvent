package com.syncevent.users.api;

import com.syncevent.users.domain.UserRole;

import jakarta.validation.constraints.NotNull;

public record AssignUserRoleRequest(@NotNull UserRole role) {
}
