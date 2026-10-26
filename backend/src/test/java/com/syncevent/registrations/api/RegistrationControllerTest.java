package com.syncevent.registrations.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import com.syncevent.events.infrastructure.persistence.SpringDataEventRepository;
import com.syncevent.participants.infrastructure.ParticipantChangeHistoryRepository;
import com.syncevent.participants.infrastructure.SpringDataParticipantRepository;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationControllerTest {

    private static final String OPERATOR_EMAIL = "registration-operator@example.com";
    private static final String OPERATOR_PASSWORD = "operator-password-123";

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

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Autowired
    private SpringDataParticipantRepository participantRepository;

    @Autowired
    private ParticipantChangeHistoryRepository participantChangeHistoryRepository;

    private String operatorToken;

    @BeforeEach
    void prepareOperatorAndClearRegistrationData() throws Exception {
        jdbcTemplate.update("DELETE FROM event_registrations");
        jdbcTemplate.update("DELETE FROM event_cancellation_history");
        jdbcTemplate.update("DELETE FROM event_postponement_history");
        jdbcTemplate.update("DELETE FROM events");
        participantChangeHistoryRepository.deleteAll();
        participantRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");

        userPersistencePort.save(UserAccount.createManaged(
                OPERATOR_EMAIL,
                passwordEncoder.encode(OPERATOR_PASSWORD),
                Set.of(UserRole.OPERADOR),
                LocalDateTime.now(clock)));
        operatorToken = login(OPERATOR_EMAIL, OPERATOR_PASSWORD);
    }

    @Test
    void creaInscripcionActivaConTokenQrUnico() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-01T09:00:00", "2027-05-01T12:00:00");
        UUID participantId = createParticipant("10001", "ana@example.com");

        MvcResult result = mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(participantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.participantId").value(participantId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVA"))
                .andExpect(jsonPath("$.qrToken").isString())
                .andReturn();

        String qrToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("qrToken").asText();
        org.junit.jupiter.api.Assertions.assertEquals(43, qrToken.length());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = 'REGISTRATION_CREATED'",
                Integer.class));
    }

    @Test
    void rechazaInscripcionDuplicada() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-01T09:00:00", "2027-05-01T12:00:00");
        UUID participantId = createParticipant("10002", "duplicate@example.com");
        createRegistration(eventId, participantId).andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(participantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Inscripción no permitida"));

        org.junit.jupiter.api.Assertions.assertEquals(1, activeCount(eventId));
    }

    @Test
    void rechazaInscripcionConAforoCompleto() throws Exception {
        UUID eventId = createPublishedEvent(1, "2027-05-02T09:00:00", "2027-05-02T12:00:00");
        UUID firstParticipant = createParticipant("10003", "first@example.com");
        UUID secondParticipant = createParticipant("10004", "second@example.com");
        createRegistration(eventId, firstParticipant).andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(secondParticipant)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El evento alcanzó su aforo máximo."));

        org.junit.jupiter.api.Assertions.assertEquals(1, activeCount(eventId));
    }

    @Test
    void rechazaCruceDeHorarioEIndicaElEventoEnConflicto() throws Exception {
        UUID participantId = createParticipant("10005", "schedule@example.com");
        UUID firstEvent = createPublishedEvent(10, "2027-05-03T09:00:00", "2027-05-03T12:00:00");
        UUID conflictingEvent = createPublishedEvent(10, "2027-05-03T11:00:00", "2027-05-03T14:00:00");
        createRegistration(firstEvent, participantId).andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{eventId}/registrations", conflictingEvent)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(participantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cruce de horario"))
                .andExpect(jsonPath("$.conflictingEventId").value(firstEvent.toString()))
                .andExpect(jsonPath("$.conflictingEventName").value("Nuevo evento"));
    }

    @Test
    void rechazaInscripcionCuandoEventoNoEstaPublicado() throws Exception {
        UUID eventId = createDraftEvent(10, "2027-05-04T09:00:00", "2027-05-04T12:00:00");
        UUID participantId = createParticipant("10006", "draft@example.com");

        mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(participantId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El evento debe estar PUBLICADO para aceptar inscripciones; su estado actual es BORRADOR."));
    }

    @Test
    void soloOperadorOAdministradorPuedeCrearInscripciones() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-05T09:00:00", "2027-05-05T12:00:00");
        UUID participantId = createParticipant("10007", "guest@example.com");
        userPersistencePort.save(UserAccount.createManaged(
                "registration-guest@example.com",
                passwordEncoder.encode("guest-password-123"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));
        String guestToken = login("registration-guest@example.com", "guest-password-123");

        mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest(participantId)))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0, activeCount(eventId));
    }

    @Test
    void dosInscripcionesSimultaneasNoSuperanElUltimoCupo() throws Exception {
        UUID eventId = createPublishedEvent(1, "2027-05-06T09:00:00", "2027-05-06T12:00:00");
        UUID firstParticipant = createParticipant("10008", "race1@example.com");
        UUID secondParticipant = createParticipant("10009", "race2@example.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> firstResult = executor.submit(() -> concurrentRegistrationStatus(
                    eventId, firstParticipant, ready, start));
            Future<Integer> secondResult = executor.submit(() -> concurrentRegistrationStatus(
                    eventId, secondParticipant, ready, start));
            ready.await();
            start.countDown();

            int firstStatus = firstResult.get();
            int secondStatus = secondResult.get();
            org.junit.jupiter.api.Assertions.assertTrue(
                    firstStatus == 201 && secondStatus == 409 || firstStatus == 409 && secondStatus == 201,
                    "Una inscripción debe tener éxito y la otra debe rechazarse por aforo.");
            org.junit.jupiter.api.Assertions.assertEquals(1, activeCount(eventId));
        } finally {
            executor.shutdownNow();
        }
    }

    private int concurrentRegistrationStatus(
            UUID eventId, UUID participantId, CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            return mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registrationRequest(participantId)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private org.springframework.test.web.servlet.ResultActions createRegistration(UUID eventId, UUID participantId)
            throws Exception {
        return mockMvc.perform(post("/api/events/{eventId}/registrations", eventId)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registrationRequest(participantId)));
    }

    private UUID createPublishedEvent(int capacity, String startAt, String endAt) throws Exception {
        UUID eventId = createDraftEvent(capacity, startAt, endAt);
        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PUBLICADO"}
                                """))
                .andExpect(status().isOk());
        return eventId;
    }

    private UUID createDraftEvent(int capacity, String startAt, String endAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Nuevo evento",
                                  "description":"Prueba de inscripción",
                                  "type":"CAPACITACION",
                                  "modality":"PRESENCIAL",
                                  "startAt":"%s",
                                  "endAt":"%s",
                                  "location":"Sede %s",
                                  "capacity":%d,
                                  "durationHours":3.0
                                }
                                """.formatted(startAt, endAt, UUID.randomUUID(), capacity)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createParticipant(String documentNumber, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/participants")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType":"CC",
                                  "documentNumber":"%s",
                                  "firstName":"Participante",
                                  "lastName":"Prueba",
                                  "email":"%s",
                                  "phone":"3001234567"
                                }
                                """.formatted(documentNumber, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
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

    private String registrationRequest(UUID participantId) {
        return """
                {"participantId":"%s"}
                """.formatted(participantId);
    }

    private String bearer() {
        return "Bearer " + operatorToken;
    }

    private int activeCount(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND status = 'ACTIVA'",
                Integer.class,
                eventId);
    }
}
