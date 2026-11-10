package com.syncevent.audit.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLog(
        Long id,
        LocalDateTime occurredAt,
        UUID userId,
        String userEmail,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String result,
        String reason,
        String details,
        String httpMethod,
        String httpPath,
        Integer statusCode) {

    public static final String SUCCESS = "EXITO";
    public static final String FAILURE = "FALLO";

    public static AuditLog create(
            UUID userId,
            String userEmail,
            String action,
            String entityType,
            String entityId,
            String ipAddress,
            String result,
            String reason,
            String details,
            String httpMethod,
            String httpPath,
            Integer statusCode,
            LocalDateTime occurredAt) {
        return new AuditLog(
                null,
                occurredAt,
                userId,
                normalizeEmail(userEmail),
                trimTo(action, 80, "ACCION_DESCONOCIDA"),
                trimTo(entityType, 40, "SISTEMA"),
                trimTo(entityId, 120, null),
                trimTo(ipAddress, 45, null),
                normalizeResult(result),
                trimTo(reason, 2000, null),
                trimTo(details, 1000, null),
                trimTo(httpMethod, 10, null),
                trimTo(httpPath, 500, null),
                statusCode);
    }

    public AuditLog withOccurredAt(LocalDateTime value) {
        return new AuditLog(
                id,
                value,
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

    private static String normalizeEmail(String value) {
        String normalized = trimTo(value, 254, null);
        return normalized == null ? "anonimo" : normalized.trim().toLowerCase();
    }

    private static String normalizeResult(String value) {
        return FAILURE.equalsIgnoreCase(value) ? FAILURE : SUCCESS;
    }

    private static String trimTo(String value, int maxLength, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
