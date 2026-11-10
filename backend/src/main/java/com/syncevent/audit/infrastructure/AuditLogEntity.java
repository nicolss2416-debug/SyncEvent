package com.syncevent.audit.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.audit.domain.AuditLog;
import com.syncevent.users.infrastructure.UserAccountEntity;

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
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private UserAccountEntity user;

    @Column(name = "user_email", nullable = false, updatable = false, length = 254)
    private String userEmail;

    @Column(nullable = false, updatable = false, length = 80)
    private String action;

    @Column(name = "entity_type", nullable = false, updatable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", updatable = false, length = 120)
    private String entityId;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false, length = 20)
    private String result;

    @Column(updatable = false, length = 2000)
    private String reason;

    @Column(updatable = false, length = 1000)
    private String details;

    @Column(name = "http_method", updatable = false, length = 10)
    private String httpMethod;

    @Column(name = "http_path", updatable = false, length = 500)
    private String httpPath;

    @Column(name = "status_code", updatable = false)
    private Integer statusCode;

    protected AuditLogEntity() {
    }

    static AuditLogEntity fromDomain(AuditLog auditLog) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = auditLog.id();
        entity.occurredAt = auditLog.occurredAt();
        entity.userId = auditLog.userId();
        entity.userEmail = auditLog.userEmail();
        entity.action = auditLog.action();
        entity.entityType = auditLog.entityType();
        entity.entityId = auditLog.entityId();
        entity.ipAddress = auditLog.ipAddress();
        entity.result = auditLog.result();
        entity.reason = auditLog.reason();
        entity.details = auditLog.details();
        entity.httpMethod = auditLog.httpMethod();
        entity.httpPath = auditLog.httpPath();
        entity.statusCode = auditLog.statusCode();
        return entity;
    }

    AuditLog toDomain() {
        return new AuditLog(
                id,
                occurredAt,
                userId,
                userEmail,
                action,
                entityType,
                entityId,
                ipAddress,
                result,
                reason,
                details,
                httpMethod,
                httpPath,
                statusCode);
    }
}
