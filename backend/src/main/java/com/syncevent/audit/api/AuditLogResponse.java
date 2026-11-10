package com.syncevent.audit.api;

import java.time.LocalDateTime;

import com.syncevent.audit.domain.AuditLog;

public record AuditLogResponse(
        Long id,
        LocalDateTime timestamp,
        String userEmail,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String result,
        String reason,
        String details) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.id(),
                auditLog.occurredAt(),
                auditLog.userEmail(),
                auditLog.action(),
                auditLog.entityType(),
                auditLog.entityId(),
                auditLog.ipAddress(),
                auditLog.result(),
                auditLog.reason(),
                auditLog.details());
    }
}
