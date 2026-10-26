package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserRole;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_accounts_email", columnNames = "email")
})
public class UserAccountEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserAccountEntity() {
    }

    static UserAccountEntity fromDomain(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = account.getId();
        entity.email = account.getEmail();
        entity.passwordHash = account.getPasswordHash();
        entity.roles = new HashSet<>(account.getRoles());
        entity.active = account.isActive();
        entity.createdAt = account.getCreatedAt();
        return entity;
    }

    UserAccount toDomain() {
        return UserAccount.reconstitute(id, email, passwordHash, roles, active, createdAt);
    }
}
