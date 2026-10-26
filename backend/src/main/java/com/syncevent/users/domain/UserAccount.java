package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

public final class UserAccount {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Set<UserRole> roles;
    private final boolean active;
    private final LocalDateTime createdAt;

    private UserAccount(
            UUID id,
            String email,
            String passwordHash,
            Set<UserRole> roles,
            boolean active,
            LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = Set.copyOf(roles);
        this.active = active;
        this.createdAt = createdAt;
    }

    public static UserAccount register(String email, String passwordHash, LocalDateTime createdAt) {
        return new UserAccount(
                UUID.randomUUID(),
                email.trim().toLowerCase(Locale.ROOT),
                passwordHash,
                Set.of(UserRole.INVITADO),
                true,
                createdAt);
    }

    public static UserAccount createManaged(
            String email, String passwordHash, Set<UserRole> roles, LocalDateTime createdAt) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Debe asignarse al menos un rol.");
        }
        return new UserAccount(
                UUID.randomUUID(),
                email.trim().toLowerCase(Locale.ROOT),
                passwordHash,
                roles,
                true,
                createdAt);
    }

    public static UserAccount reconstitute(
            UUID id,
            String email,
            String passwordHash,
            Set<UserRole> roles,
            boolean active,
            LocalDateTime createdAt) {
        return new UserAccount(id, email, passwordHash, roles, active, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UserAccount updateEmail(String newEmail) {
        return new UserAccount(id, newEmail.trim().toLowerCase(Locale.ROOT), passwordHash, roles, active, createdAt);
    }

    public UserAccount assignRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        Set<UserRole> updatedRoles = new HashSet<>(roles);
        updatedRoles.add(role);
        return new UserAccount(id, email, passwordHash, updatedRoles, active, createdAt);
    }

    public UserAccount withActive(boolean newActive) {
        return new UserAccount(id, email, passwordHash, roles, newActive, createdAt);
    }
}
