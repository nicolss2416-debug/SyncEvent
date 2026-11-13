package com.syncevent.audit.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncevent.audit.application.AuditLogService;
import com.syncevent.audit.domain.AuditLog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Registra automáticamente en la bitácora toda operación de escritura, inicio de sesión,
 * cierre de sesión y revocación. El registro se hace después de ejecutar la petición para
 * poder guardar el resultado real (éxito o fallo) y el código HTTP.
 */
public class AuditRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditRequestFilter.class);

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final List<String> REASON_FIELDS =
            List.of("reason", "motivo", "justification", "justificacion", "cancellationReason");

    private static final List<Rule> RULES = List.of(
            new Rule("POST", "^/api/auth/login$", "INICIO_SESION", "USUARIO", false),
            new Rule("POST", "^/api/auth/register$", "REGISTRO_CUENTA", "USUARIO", false),
            new Rule("POST", "^/api/auth/logout$", "CIERRE_SESION", "USUARIO", false),
            new Rule("PUT", "^/api/auth/profile$", "PERFIL_INVITADO_COMPLETADO", "PARTICIPANTE", false),
            new Rule("POST", "^/api/admin/users$", "USUARIO_CREADO", "USUARIO", false),
            new Rule("PUT", "^/api/admin/users/[^/]+$", "USUARIO_ACTUALIZADO", "USUARIO", false),
            new Rule("POST", "^/api/admin/users/[^/]+/roles$", "ROL_ASIGNADO", "USUARIO", false),
            new Rule("PUT", "^/api/admin/users/[^/]+/roles$", "ROL_ASIGNADO", "USUARIO", false),
            new Rule("POST", "^/api/admin/users/[^/]+/participant$", "PARTICIPANTE_VINCULADO", "USUARIO", false),
            new Rule("PATCH", "^/api/admin/users/[^/]+/active$", "ESTADO_CUENTA_CAMBIADO", "USUARIO", false),
            new Rule("PUT", "^/api/admin/attendance-policy$", "POLITICA_ASISTENCIA_ACTUALIZADA", "POLITICA", false),
            new Rule("POST", "^/api/admin/fines/[^/]+/resolution$", "MULTA_RESUELTA", "MULTA", false),
            new Rule("POST", "^/api/events$", "EVENTO_CREADO", "EVENTO", false),
            new Rule("PUT", "^/api/events/[^/]+$", "EVENTO_ACTUALIZADO", "EVENTO", false),
            new Rule("PATCH", "^/api/events/[^/]+/status$", "ESTADO_EVENTO_CAMBIADO", "EVENTO", false),
            new Rule("POST", "^/api/events/[^/]+/cancellation$", "EVENTO_CANCELADO", "EVENTO", false),
            new Rule("POST", "^/api/events/[^/]+/postponements$", "EVENTO_APLAZADO", "EVENTO", false),
            new Rule("POST", "^/api/events/[^/]+/postponements/confirmation$", "APLAZAMIENTO_CONFIRMADO", "EVENTO", false),
            new Rule("POST", "^/api/events/[^/]+/monitors$", "MONITOR_ASIGNADO", "EVENTO", false),
            new Rule("DELETE", "^/api/events/[^/]+/monitors/[^/]+$", "MONITOR_RETIRADO", "EVENTO", true),
            new Rule("POST", "^/api/events/[^/]+/registrations$", "INSCRIPCION_CREADA", "INSCRIPCION", false),
            new Rule("POST", "^/api/events/[^/]+/registrations/self$", "AUTOSINSCRIPCION", "INSCRIPCION", false),
            new Rule("POST", "^/api/events/[^/]+/registrations/[^/]+/cancellation$", "INSCRIPCION_CANCELADA", "INSCRIPCION", false),
            new Rule("POST", "^/api/events/[^/]+/registrations/[^/]+/certificate$", "CERTIFICADO_EMITIDO", "CERTIFICADO", false),
            new Rule("POST", "^/api/events/[^/]+/attendance/qr$", "ASISTENCIA_QR", "ASISTENCIA", false),
            new Rule("POST", "^/api/events/[^/]+/attendance/manual$", "ASISTENCIA_MANUAL", "ASISTENCIA", false),
            new Rule("POST", "^/api/certificates/[^/]+/revoke$", "CERTIFICADO_REVOCADO", "CERTIFICADO", false),
            new Rule("POST", "^/api/participants$", "PARTICIPANTE_CREADO", "PARTICIPANTE", false),
            new Rule("PUT", "^/api/participants/[^/]+$", "PARTICIPANTE_ACTUALIZADO", "PARTICIPANTE", false));

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditRequestFilter(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method)
                && !"PATCH".equals(method) && !"DELETE".equals(method)) {
            return true;
        }
        String path = normalizePath(request);
        return path.startsWith("/graphql")
                || path.startsWith("/graphql-ws")
                || path.startsWith("/actuator")
                || path.startsWith("/vendor")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper cachingRequest
                ? cachingRequest
                : new ContentCachingRequestWrapper(request);
        Exception failure = null;
        try {
            filterChain.doFilter(wrappedRequest, response);
        } catch (IOException | ServletException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                recordSafely(wrappedRequest, response, failure);
            } catch (RuntimeException exception) {
                LOGGER.warn("No fue posible registrar el evento de auditoría", exception);
            }
        }
    }

    private void recordSafely(
            ContentCachingRequestWrapper request,
            HttpServletResponse response,
            Exception failure) {
        String method = request.getMethod();
        String path = normalizePath(request);
        JsonNode body = readJsonBody(request);
        Descriptor descriptor = resolve(method, path);
        int statusCode = failure == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String result = statusCode >= 200 && statusCode < 400 ? AuditLog.SUCCESS : AuditLog.FAILURE;
        String userEmail = resolveUserEmail(body);
        UUID userId = resolveUserId();
        String entityId = resolveEntityId(path, descriptor, response);

        AuditLog auditLog = AuditLog.create(
                userId,
                userEmail,
                descriptor.action(),
                descriptor.entityType(),
                entityId,
                resolveIpAddress(request),
                result,
                extractReason(body),
                buildDetails(method, path, statusCode, failure),
                method,
                path,
                statusCode,
                null);
        auditLogService.record(auditLog);
    }

    private Descriptor resolve(String method, String path) {
        for (Rule rule : RULES) {
            if (rule.method().equals(method) && rule.pattern().matcher(path).matches()) {
                return new Descriptor(rule.action(), rule.entityType(), rule.firstUuid());
            }
        }
        String entityType = inferEntityType(path);
        String action = switch (method) {
            case "POST" -> "CREACION_" + entityType;
            case "PUT", "PATCH" -> "ACTUALIZACION_" + entityType;
            case "DELETE" -> "ELIMINACION_" + entityType;
            default -> method + "_" + entityType;
        };
        return new Descriptor(action, entityType, false);
    }

    private String inferEntityType(String path) {
        if (path.contains("/registrations")) {
            return "INSCRIPCION";
        }
        if (path.contains("/attendance")) {
            return "ASISTENCIA";
        }
        if (path.contains("/certificates")) {
            return "CERTIFICADO";
        }
        if (path.contains("/fines")) {
            return "MULTA";
        }
        if (path.contains("/attendance-policy")) {
            return "POLITICA";
        }
        if (path.contains("/users")) {
            return "USUARIO";
        }
        if (path.contains("/participants")) {
            return "PARTICIPANTE";
        }
        if (path.contains("/events")) {
            return "EVENTO";
        }
        return "SISTEMA";
    }

    private String resolveUserEmail(JsonNode body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
                return jwt.getSubject();
            }
            if (authentication.getName() != null && !authentication.getName().isBlank()) {
                return authentication.getName();
            }
        }
        if (body != null) {
            JsonNode email = body.get("email");
            if (email != null && email.isTextual() && !email.asText().isBlank()) {
                return email.asText().trim().toLowerCase(Locale.ROOT);
            }
        }
        return "anonimo";
    }

    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String uid = jwt.getClaimAsString("uid");
            if (uid != null && !uid.isBlank()) {
                try {
                    return UUID.fromString(uid);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String resolveEntityId(String path, Descriptor descriptor, HttpServletResponse response) {
        String fromLocation = lastUuid(response.getHeader("Location"));
        if (fromLocation != null) {
            return fromLocation;
        }
        return descriptor.firstUuid() ? firstUuid(path) : lastUuid(path);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            return first.length() <= 45 ? first : first.substring(0, 45);
        }
        String remoteAddress = request.getRemoteAddr();
        return remoteAddress != null && remoteAddress.length() <= 45
                ? remoteAddress
                : remoteAddress == null ? null : remoteAddress.substring(0, 45);
    }

    private JsonNode readJsonBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (IOException exception) {
            return null;
        }
    }

    private String extractReason(JsonNode body) {
        if (body == null) {
            return null;
        }
        for (String field : REASON_FIELDS) {
            JsonNode value = body.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private String buildDetails(String method, String path, int statusCode, Exception failure) {
        StringBuilder details = new StringBuilder()
                .append(method).append(' ').append(path)
                .append(" -> HTTP ").append(statusCode);
        if (path.endsWith("/attendance/manual")) {
            details.append(" (check-in manual)");
        }
        if (failure != null && failure.getMessage() != null) {
            details.append(": ").append(failure.getMessage());
        }
        return details.toString();
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    private String lastUuid(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = UUID_PATTERN.matcher(value);
        String last = null;
        while (matcher.find()) {
            last = matcher.group();
        }
        return last;
    }

    private String firstUuid(String value) {
        Matcher matcher = UUID_PATTERN.matcher(value);
        return matcher.find() ? matcher.group() : null;
    }

    private record Rule(String method, Pattern pattern, String action, String entityType, boolean firstUuid) {
        private Rule(String method, String pattern, String action, String entityType, boolean firstUuid) {
            this(method, Pattern.compile(pattern), action, entityType, firstUuid);
        }
    }

    private record Descriptor(String action, String entityType, boolean firstUuid) {
    }
}
