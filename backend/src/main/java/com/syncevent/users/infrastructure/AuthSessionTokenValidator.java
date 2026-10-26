package com.syncevent.users.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.syncevent.users.domain.AuthSessionPersistencePort;

@Component
public class AuthSessionTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_SESSION =
            new OAuth2Error("invalid_token", "La sesión expiró o fue revocada.", null);

    private final AuthSessionPersistencePort sessionPersistencePort;
    private final Clock clock;
    private final Duration idleTimeout;

    public AuthSessionTokenValidator(
            AuthSessionPersistencePort sessionPersistencePort,
            Clock clock,
            @Value("${app.auth.jwt.idle-timeout}") Duration idleTimeout) {
        this.sessionPersistencePort = sessionPersistencePort;
        this.clock = clock;
        if (idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("El tiempo de inactividad debe ser positivo.");
        }
        this.idleTimeout = idleTimeout;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sessionClaim = token.getClaimAsString("sid");
        if (sessionClaim == null) {
            return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionClaim);
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean active = sessionPersistencePort.recordActivityIfValid(
                sessionId, now.minus(idleTimeout), now);
        return active
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
}
