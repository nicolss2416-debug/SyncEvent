package com.syncevent.audit.api;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve los datos del actor que ejecuta una operación auditada a partir del contexto
 * de seguridad y de la petición HTTP actual.
 */
public final class AuditActorResolver {

    private AuditActorResolver() {
    }

    public static String resolveEmail(JsonNode body) {
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

    public static UUID resolveUserId() {
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

    public static String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
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

    public static String resolveCurrentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return resolveIpAddress(attributes.getRequest());
        }
        return null;
    }
}
