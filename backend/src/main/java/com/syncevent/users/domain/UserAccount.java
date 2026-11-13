package com.syncevent.users.domain;

import java.time.LocalDateTime;
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
    private final LocalDateTime passwordChangedAt;
    private final UUID participantId;

    private UserAccount(
            UUID id,
            String email,
            String passwordHash,
            Set<UserRole> roles,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime passwordChangedAt,
            UUID participantId) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = Set.copyOf(roles);
        this.active = active;
        this.createdAt = createdAt;
        this.passwordChangedAt = passwordChangedAt;
        this.participantId = participantId;
    }

    public static UserAccount register(String email, String passwordHash, LocalDateTime createdAt) {
        return new UserAccount(
                UUID.randomUUID(),
                email.trim().toLowerCase(Locale.ROOT),
                passwordHash,
                Set.of(UserRole.INVITADO),
                true,
                createdAt,
                createdAt,
                null);
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
                createdAt,
                createdAt,
                null);
    }

    public static UserAccount reconstitute(
            UUID id,
            String email,
            String passwordHash,
            Set<UserRole> roles,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime passwordChangedAt,
            UUID participantId) {
        Set<UserRole> effectiveRoles = (roles == null || roles.isEmpty())
                ? Set.of(UserRole.INVITADO)
                : Set.copyOf(roles);
        LocalDateTime effectivePasswordChangedAt = passwordChangedAt == null ? createdAt : passwordChangedAt;
        return new UserAccount(
                id,
                email,
                passwordHash,
                effectiveRoles,
                active,
                createdAt,
                effectivePasswordChangedAt,
                participantId);
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

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public UserAccount updateEmail(String newEmail) {
        return new UserAccount(
                id,
                newEmail.trim().toLowerCase(Locale.ROOT),
                passwordHash,
                roles,
                active,
                createdAt,
                passwordChangedAt,
                participantId);
    }

    public UserAccount assignRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        return new UserAccount(id, email, passwordHash, Set.of(role), active, createdAt, passwordChangedAt, participantId);
    }

    public UserAccount withActive(boolean newActive) {
        return new UserAccount(id, email, passwordHash, roles, newActive, createdAt, passwordChangedAt, participantId);
    }

    public UserAccount withPassword(String newPasswordHash, LocalDateTime changedAt) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        return new UserAccount(
                id,
                email,
                newPasswordHash,
                roles,
                active,
                createdAt,
                changedAt == null ? LocalDateTime.now() : changedAt,
                participantId);
    }

    public UserAccount linkParticipant(UUID newParticipantId) {
        if (newParticipantId == null) {
            throw new IllegalArgumentException("El participante es obligatorio.");
        }
        if (participantId != null && !participantId.equals(newParticipantId)) {
            throw new IllegalStateException("La cuenta ya está vinculada a otro participante.");
        }
        return new UserAccount(
                id,
                email,
                passwordHash,
                roles,
                active,
                createdAt,
                passwordChangedAt,
                newParticipantId);
    }
}
