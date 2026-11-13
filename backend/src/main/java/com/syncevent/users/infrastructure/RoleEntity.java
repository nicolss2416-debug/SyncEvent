package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;

import com.syncevent.users.domain.RoleInfo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RoleEntity() {
    }

    public RoleEntity(String name, String displayName, String description, LocalDateTime createdAt) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static RoleEntity fromDomain(RoleInfo info) {
        return new RoleEntity(info.name(), info.displayName(), info.description(), LocalDateTime.now());
    }

    public RoleInfo toDomain() {
        return new RoleInfo(name, displayName, description);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
