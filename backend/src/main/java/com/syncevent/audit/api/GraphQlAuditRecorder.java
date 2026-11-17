package com.syncevent.audit.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.syncevent.audit.application.GraphQlAuditService;

/**
 * Punto de entrada para auditar mutaciones GraphQL. Resuelve el actor desde el contexto
 * de seguridad y delega el registro en la capa de aplicación sin propagar fallos de auditoría.
 */
@Component
public class GraphQlAuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlAuditRecorder.class);

    private final GraphQlAuditService graphQlAuditService;

    public GraphQlAuditRecorder(GraphQlAuditService graphQlAuditService) {
        this.graphQlAuditService = graphQlAuditService;
    }

    public void success(
            String action,
            String entityType,
            Object entityId,
            String reason,
            String details) {
        record(action, entityType, entityId, reason, details, true);
    }

    public void failure(
            String action,
            String entityType,
            Object entityId,
            String reason,
            String details) {
        record(action, entityType, entityId, reason, details, false);
    }

    private void record(
            String action,
            String entityType,
            Object entityId,
            String reason,
            String details,
            boolean success) {
        try {
            GraphQlAuditService.AuditActor actor = new GraphQlAuditService.AuditActor(
                    AuditActorResolver.resolveEmail(null),
                    AuditActorResolver.resolveUserId(),
                    AuditActorResolver.resolveCurrentIpAddress());
            String id = entityId == null ? null : entityId.toString();
            if (success) {
                graphQlAuditService.recordSuccess(actor, action, entityType, id, reason, details);
            } else {
                graphQlAuditService.recordFailure(actor, action, entityType, id, reason, details);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("No fue posible registrar la auditoría de la mutación GraphQL", exception);
        }
    }
}
