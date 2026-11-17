package com.syncevent.users.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncevent.users.application.AuthRateLimiter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String EMAIL = "auth-test@example.com";
    private static final String PASSWORD = "safe-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthRateLimiter authRateLimiter;

    @BeforeEach
    void clearAuthData() {
        authRateLimiter.clearAll();
        jdbcTemplate.update("DELETE FROM password_reset_tokens");
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");
    }

    @Test
    void registraCuentaComoInvitadoYAlmacenaHashDeContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.roles[0]").value("INVITADO"));

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM user_accounts WHERE email = ?",
                String.class,
                EMAIL);
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, storedHash);
        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("REGISTER"));
    }

    @Test
    void registroCompletoCreaYVinculaPerfilParaAutinscripcion() throws Exception {
        String documentNumber = "GUEST-" + UUID.randomUUID();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","documentType":"CC","documentNumber":"%s","firstName":"Ana","lastName":"Pérez","phone":"3001234567"}
                                """.formatted(EMAIL, PASSWORD, documentNumber)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("INVITADO"));

        String token = login(EMAIL, PASSWORD);
        mockMvc.perform(get("/api/participants/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value(documentNumber.toUpperCase(java.util.Locale.ROOT)))
                .andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void rechazaCorreoDuplicadoSinCrearOtraCuenta() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest("AUTH-TEST@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Correo ya registrado"));

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_accounts", Integer.class));
    }

    @Test
    void inicioDeSesionEntregaJwtQueAutorizaPorRol() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);

        // El endpoint /api/events requiere autenticación; un token válido lo supera
        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaContrasenaIncorrectaConRespuestaGenerica() throws Exception {
        register(EMAIL, PASSWORD);

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticación fallida"))
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("unknown@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticación fallida"))
                .andReturn();

        // Las respuestas deben ser indistinguibles para evitar enumeración de usuarios
        JsonNode wrongPasswordBody = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
        JsonNode unknownEmailBody = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(wrongPasswordBody.get("detail"), unknownEmailBody.get("detail"));
        org.junit.jupiter.api.Assertions.assertEquals(2, countActivity("LOGIN_FAILED"));
    }

    @Test
    void cierreDeSesionRevocaTokenInmediatamente() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Reintentar usar el mismo token ahora debe ser rechazado
        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("LOGOUT"));
    }

    @Test
    void cambioDeContrasenaRevocaSesionesPreviasYExigeNuevaClave() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "%s",
                                  "newPassword": "new-super-secret-password-123"
                                }
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        // Sesión anterior invalidada
        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Antigua contraseña ya no sirve
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());

        // Nueva contraseña funciona
        login(EMAIL, "new-super-secret-password-123");
        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("USER_PASSWORD_CHANGED"));
    }

    @Test
    void usuarioNoPuedeCambiarContrasenaConContrasenaActualIncorrecta() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrong-password",
                                  "newPassword": "new-super-secret-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitarRecuperacionGeneraOtpYPermiteRestablecerContrasena() throws Exception {
        register(EMAIL, PASSWORD);

        MvcResult forgotResult = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Si el correo está registrado, recibirás un código de verificación de 6 dígitos con vigencia de 15 minutos."))
                .andExpect(jsonPath("$.devOtp").isNotEmpty())
                .andReturn();

        String otp = objectMapper.readTree(forgotResult.getResponse().getContentAsString())
                .get("devOtp").asText();

        String nuevaClave = "brand-new-password-123";

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "otp": "%s",
                                  "newPassword": "%s"
                                }
                                """.formatted(EMAIL, otp, nuevaClave)))
                .andExpect(status().isNoContent());

        // Iniciar sesión con la nueva clave tiene éxito
        login(EMAIL, nuevaClave);

        // La clave anterior queda revocada
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitarRecuperacionConCorreoNoRegistradoRetornaMismoMensajeGenericoSinExponerOtp() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-existe@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Si el correo está registrado, recibirás un código de verificación de 6 dígitos con vigencia de 15 minutos."))
                .andExpect(jsonPath("$.devOtp").doesNotExist());
    }

    @Test
    void restablecerContrasenaConOtpInvalidoFalla() throws Exception {
        register(EMAIL, PASSWORD);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(EMAIL)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "otp": "000000",
                                  "newPassword": "brand-new-password-123"
                                }
                                """.formatted(EMAIL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proteccionFuerzaBrutaBloqueaLoginTrasMultiplesIntentosFallidos() throws Exception {
        register(EMAIL, PASSWORD);

        // 5 intentos fallidos consecutivos para la misma cuenta
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginRequest(EMAIL, "wrong-password-" + i)))
                    .andExpect(status().isUnauthorized());
        }

        // El 6to intento debe ser bloqueado por HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(EMAIL, PASSWORD)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Límite de intentos excedido"));
    }

    @Test
    void proteccionFuerzaBrutaBloqueaResetPasswordTrasMultiplesIntentosFallidos() throws Exception {
        register(EMAIL, PASSWORD);

        // 5 intentos fallidos de OTP
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "otp": "00000%d",
                                      "newPassword": "brand-new-password-123"
                                    }
                                    """.formatted(EMAIL, i)))
                    .andExpect(status().isBadRequest());
        }

        // El 6to intento debe ser rechazado con 429
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "otp": "123456",
                                  "newPassword": "brand-new-password-123"
                                }
                                """.formatted(EMAIL)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Límite de intentos excedido"));
    }

    private void register(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(email, password)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private int countActivity(String activity) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = ?",
                Integer.class,
                activity);
    }

    private String registerRequest(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private String loginRequest(String email, String password) {
        return registerRequest(email, password);
    }
}
