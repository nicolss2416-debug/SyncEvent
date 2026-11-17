package com.syncevent.audit.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.syncevent.audit.domain.AuditLog;

/**
 * Registra en la bitácora las mutaciones ejecutadas a través de GraphQL. Se invoca desde
 * el adaptador GraphQL y reutiliza el mecanismo de persistencia existente de auditoría.
 */
@Service
public class GraphQlAuditService {

    private static final String HTTP_METHOD = "POST";
    private static final String HTTP_PATH = "/graphql";
    private static final int SUCCESS_STATUS = 200;

    private final AuditLogService auditLogService;

    public GraphQlAuditService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public void recordSuccess(
            AuditActor actor,
            String action,
            String entityType,
            String entityId,
            String reason,
            String details) {
        record(actor, action, entityType, entityId, reason, details, AuditLog.SUCCESS, SUCCESS_STATUS);
    }

    public void recordFailure(
            AuditActor actor,
            String action,
            String entityType,
            String entityId,
            String reason,
            String details) {
        record(actor, action, entityType, entityId, reason, details, AuditLog.FAILURE, null);
    }

    private void record(
            AuditActor actor,
            String action,
            String entityType,
            String entityId,
            String reason,
            String details,
            String result,
            Integer statusCode) {
        AuditLog auditLog = AuditLog.create(
                actor.userId(),
                actor.email(),
                action,
                entityType,
                entityId,
                actor.ipAddress(),
                result,
                reason,
                details,
                HTTP_METHOD,
                HTTP_PATH,
                statusCode,
                null);
        auditLogService.record(auditLog);
    }

    public record AuditActor(String email, UUID userId, String ipAddress) {
    }
}
