package com.syncevent.users.api;

import java.util.Set;

import com.syncevent.users.domain.UserRole;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRolesRequest(@NotEmpty Set<@NotNull UserRole> roles) {
}
