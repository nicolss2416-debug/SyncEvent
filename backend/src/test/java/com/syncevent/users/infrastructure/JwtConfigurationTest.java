package com.syncevent.users.infrastructure;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtConfigurationTest {

    private final JwtConfiguration jwtConfiguration = new JwtConfiguration();

    @Test
    void produccionFallaSiFaltaJwtSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> jwtConfiguration.jwtSecretKey("", env));

        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    @Test
    void produccionFallaSiUsaClaveInseguraPorDefecto() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("JWT_SECRET", JwtConfiguration.INSECURE_DEFAULT_SECRET);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> jwtConfiguration.jwtSecretKey(JwtConfiguration.INSECURE_DEFAULT_SECRET, env));

        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    @Test
    void produccionTieneExitoConClaveValidaYDistintaALaPredeterminada() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        byte[] customKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            customKey[i] = (byte) (i + 1);
        }
        String customBase64 = Base64.getEncoder().encodeToString(customKey);
        env.setProperty("JWT_SECRET", customBase64);

        SecretKey secretKey = jwtConfiguration.jwtSecretKey(customBase64, env);
        assertNotNull(secretKey);
    }

    @Test
    void desarrolloTieneExitoConConfiguracionPorDefecto() {
        MockEnvironment env = new MockEnvironment();
        // Perfil por defecto (no prod)
        SecretKey secretKey = jwtConfiguration.jwtSecretKey(JwtConfiguration.INSECURE_DEFAULT_SECRET, env);
        assertNotNull(secretKey);
    }
}
