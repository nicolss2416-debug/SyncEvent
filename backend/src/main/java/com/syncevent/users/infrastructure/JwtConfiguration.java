package com.syncevent.users.infrastructure;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class JwtConfiguration {

    public static final String INSECURE_DEFAULT_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Bean
    SecretKey jwtSecretKey(
            @Value("${app.auth.jwt.secret:}") String encodedSecret,
            Environment environment) {
        boolean isProduction = environment.acceptsProfiles(Profiles.of("prod", "production"));
        if (isProduction) {
            String rawEnvSecret = environment.getProperty("JWT_SECRET");
            if (rawEnvSecret == null || rawEnvSecret.isBlank() || INSECURE_DEFAULT_SECRET.equals(rawEnvSecret.trim())) {
                throw new IllegalStateException(
                        "En entorno de producción (prod) es obligatorio configurar la variable de entorno JWT_SECRET con un valor seguro y diferente al predeterminado.");
            }
        }

        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET no puede estar vacío.");
        }

        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET debe ser una clave Base64 válida.", exception);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 256 bits.");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.auth.jwt.issuer}") String issuer,
            AuthSessionTokenValidator sessionTokenValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                sessionTokenValidator));
        return decoder;
    }
}
