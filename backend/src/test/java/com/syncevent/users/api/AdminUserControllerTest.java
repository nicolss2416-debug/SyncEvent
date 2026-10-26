package com.syncevent.users.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;
import com.syncevent.users.application.AdminUserService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserPersistencePort userPersistencePort;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private Clock clock;

    private UUID adminId;
    private String adminToken;

    @BeforeEach
    void createBootstrapAdminForTest() throws Exception {
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");

        UserAccount admin = userPersistencePort.save(UserAccount.createManaged(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD),
                Set.of(UserRole.ADMINISTRADOR),
                LocalDateTime.now(clock)));
        adminId = admin.getId();
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void administradorPuedeCrearConsultarYActualizarCorreoDeUsuario() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "operator@example.com",
                                  "password": "operator-password",
                                  "roles": ["OPERADOR"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("operator@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("OPERADOR"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        UUID createdId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].email").value("admin@example.com"));

        mockMvc.perform(put("/api/admin/users/{userId}", createdId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-operator@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-operator@example.com"));
    }

    @Test
    void asignaRolesSinQuitarLosQueYaTenia() throws Exception {
        UUID userId = createUser("operator@example.com", "OPERADOR");

        mockMvc.perform(post("/api/admin/users/{userId}/roles", userId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"MONITOR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("MONITOR"))
                .andExpect(jsonPath("$.roles[1]").value("OPERADOR"));

        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("USER_ROLE_ADDED"));
    }

    @Test
    void desactivarCuentaRevocaSuAccesoInmediatamente() throws Exception {
        UUID userId = createUser("guest@example.com", "INVITADO");
        String userToken = login("guest@example.com", "guest-password-123");

        mockMvc.perform(patch("/api/admin/users/{userId}/active", userId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertEquals(1, countActivity("USER_DEACTIVATED"));
    }

    @Test
    void noPermiteDesactivarAlUltimoAdministradorActivo() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userId}/active", adminId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Operación no permitida"));
    }

    @Test
    void soloAdministradorPuedeGestionarCuentas() throws Exception {
        UUID userId = createUser("guest@example.com", "INVITADO");
        String guestToken = login("guest@example.com", "guest-password-123");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/users/{userId}/roles", userId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"OPERADOR"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}", UUID.randomUUID())
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Usuario no encontrado"));
    }

    @Test
    void bootstrapAdminEsIdempotenteYNoCreaCredencialesDuplicadas() throws Exception {
        adminUserService.provisionBootstrapAdministrator("bootstrap@example.com", "bootstrap-password");
        adminUserService.provisionBootstrapAdministrator("bootstrap@example.com", "bootstrap-password");

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_accounts WHERE email = 'bootstrap@example.com'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM user_account_roles r
                JOIN user_accounts u ON u.id = r.user_id
                WHERE u.email = 'bootstrap@example.com' AND r.role = 'ADMINISTRADOR'
                """,
                Integer.class));
        login("bootstrap@example.com", "bootstrap-password");
    }

    @Test
    void bootstrapNoElevaUnaCuentaInvitadoExistente() {
        userPersistencePort.save(UserAccount.createManaged(
                "already-registered@example.com",
                passwordEncoder.encode("guest-password"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> adminUserService.provisionBootstrapAdministrator(
                        "already-registered@example.com", "new-admin-password"));
        org.junit.jupiter.api.Assertions.assertEquals(
                Set.of(UserRole.INVITADO),
                userPersistencePort.findByEmail("already-registered@example.com").orElseThrow().getRoles());
    }

    private UUID createUser(String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "guest-password-123",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("id").asText());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }

    private int countActivity(String activity) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = ?",
                Integer.class,
                activity);
    }
}
