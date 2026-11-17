package com.syncevent.audit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GraphQlAuditIntegrationTest {

    private static final String ADMIN_EMAIL = "graphql-audit-admin@example.com";
    private static final String ADMIN_PASSWORD = "graphql-audit-password-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserPersistencePort userPersistencePort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    private String adminToken;

    @BeforeEach
    void prepareData() throws Exception {
        jdbcTemplate.update("DELETE FROM audit_log");
        jdbcTemplate.update("DELETE FROM certificate_documents");
        jdbcTemplate.update("DELETE FROM participant_fines");
        jdbcTemplate.update("DELETE FROM attendance_absences");
        jdbcTemplate.update("DELETE FROM event_attendance");
        jdbcTemplate.update("DELETE FROM registration_cancellation_history");
        jdbcTemplate.update("DELETE FROM event_registrations");
        jdbcTemplate.update("DELETE FROM event_cancellation_history");
        jdbcTemplate.update("DELETE FROM event_postponement_history");
        jdbcTemplate.update("DELETE FROM event_monitor_assignments");
        jdbcTemplate.update("DELETE FROM events");
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");

        userPersistencePort.save(UserAccount.createManaged(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD),
                Set.of(UserRole.ADMINISTRADOR),
                LocalDateTime.now(clock)));
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    void registraMutacionesGraphQlYConservaLaAuditoriaRest() throws Exception {
        UUID eventId = createEvent(
                "Evento GraphQL",
                "Sala GraphQL",
                "2027-07-01T09:00:00",
                "2027-07-01T12:00:00");

        Map<String, Object> created = auditRow("EVENTO_CREADO", eventId.toString());
        assertEquals("EXITO", created.get("result"));
        assertEquals(ADMIN_EMAIL, created.get("user_email"));
        assertEquals("EVENTO", created.get("entity_type"));
        assertEquals("POST", created.get("http_method"));
        assertEquals("/graphql", created.get("http_path"));
        assertNotNull(created.get("occurred_at"));
        assertEquals(1, auditCount("EVENTO_CREADO", eventId.toString()));

        graphQl("""
                mutation {
                  updateEventStatus(eventId: "%s", status: "PUBLICADO") { id status }
                }
                """.formatted(eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updateEventStatus.status").value("PUBLICADO"));

        Map<String, Object> statusChanged = auditRow("ESTADO_EVENTO_CAMBIADO", eventId.toString());
        assertEquals("EXITO", statusChanged.get("result"));
        assertEquals(ADMIN_EMAIL, statusChanged.get("user_email"));

        graphQl("""
                mutation {
                  cancelEvent(eventId: "%s", reason: "Motivo de prueba") { id status }
                }
                """.formatted(eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelEvent.status").value("CANCELADO"));

        Map<String, Object> canceled = auditRow("EVENTO_CANCELADO", eventId.toString());
        assertEquals("EXITO", canceled.get("result"));
        assertEquals("Motivo de prueba", canceled.get("reason"));

        UUID missingEventId = UUID.randomUUID();
        graphQl("""
                mutation {
                  cancelEvent(eventId: "%s", reason: "Evento inexistente") { id }
                }
                """.formatted(missingEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].message").exists());

        Map<String, Object> failed = auditRow("EVENTO_CANCELADO", missingEventId.toString());
        assertEquals("FALLO", failed.get("result"));
        assertEquals(ADMIN_EMAIL, failed.get("user_email"));

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Evento REST",
                                  "type": "TALLER",
                                  "modality": "PRESENCIAL",
                                  "startAt": "2027-07-02T09:00:00",
                                  "endAt": "2027-07-02T12:00:00",
                                  "location": "Sala REST",
                                  "capacity": 10,
                                  "durationHours": 3
                                }
                                """))
                .andExpect(status().isCreated());

        Map<String, Object> restRow = jdbcTemplate.queryForMap(
                "SELECT * FROM audit_log WHERE action = ? AND http_path = ?",
                "EVENTO_CREADO", "/api/events");
        assertEquals("EXITO", restRow.get("result"));
        assertEquals(ADMIN_EMAIL, restRow.get("user_email"));
        assertEquals("POST", restRow.get("http_method"));
    }

    private UUID createEvent(String name, String location, String startAt, String endAt) throws Exception {
        MvcResult result = graphQl("""
                mutation {
                  createEvent(input: {
                    name: "%s",
                    type: "TALLER",
                    modality: "PRESENCIAL",
                    startAt: "%s",
                    endAt: "%s",
                    location: "%s",
                    capacity: 10,
                    durationHours: 3
                  }) { id status }
                }
                """.formatted(name, startAt, endAt, location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createEvent.id").exists())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("createEvent").path("id").asText());
    }

    private ResultActions graphQl(String query) throws Exception {
        return mockMvc.perform(post("/graphql")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("query", query))));
    }

    private Map<String, Object> auditRow(String action, String entityId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM audit_log WHERE action = ? AND entity_id = ?",
                action, entityId);
    }

    private int auditCount(String action, String entityId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND entity_id = ?",
                Integer.class,
                action, entityId);
        return count == null ? 0 : count;
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
}
