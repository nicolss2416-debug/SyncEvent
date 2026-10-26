package com.syncevent.users.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final Duration timeToLive;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.auth.jwt.issuer}") String issuer,
            @Value("${app.auth.jwt.access-token-ttl}") Duration timeToLive) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("La vigencia del JWT debe ser positiva.");
        }
        this.timeToLive = timeToLive;
    }

    public IssuedAccessToken issue(
            UUID userId, String email, UUID sessionId, List<String> roles) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(timeToLive))
                .subject(email)
                .id(UUID.randomUUID().toString())
                .claim("uid", userId.toString())
                .claim("sid", sessionId.toString())
                .claim("roles", roles)
                .build();

        String encoded = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
        return IssuedAccessToken.bearer(encoded, timeToLive, roles);
    }
}
