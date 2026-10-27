package com.syncevent.users.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @BeforeEach
    void clearAuthData() {
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

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void errorDeAccesoNoRevelaSiCorreoOContrasenaFueronIncorrectos() throws Exception {
        register(EMAIL, PASSWORD);

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn();
        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest("unknown@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode wrongPasswordBody = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
        JsonNode unknownEmailBody = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(wrongPasswordBody.get("detail"), unknownEmailBody.get("detail"));
        org.junit.jupiter.api.Assertions.assertEquals(2, countActivity("LOGIN_FAILED"));
    }

    @Test
    void renovarJwtConservaSesionYCerrarSesionRevocaElToken() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();
        String refreshedToken = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("LOGOUT"));
    }

    @Test
    void expiraSesionTrasTreintaMinutosSinActividad() throws Exception {
        register(EMAIL, PASSWORD);
        String token = login(EMAIL, PASSWORD);
        UUID sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM auth_sessions",
                UUID.class);
        jdbcTemplate.update(
                "UPDATE auth_sessions SET last_activity_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(31),
                sessionId);

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaRegistroConContrasenaDemasiadoCorta() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequest(EMAIL, "short")))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_accounts", Integer.class));
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
