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
import com.syncevent.attendance.application.AbsenceService;
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
    private static final String ADMIN_EMAIL = "registration-admin@example.com";
    private static final String ADMIN_PASSWORD = "admin-password-123";

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

    @Autowired
    private AbsenceService absenceService;

    private String operatorToken;
    private String adminToken;

    @BeforeEach
    void prepareOperatorAndClearRegistrationData() throws Exception {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM attendance_policy", Integer.class) == 0) {
            jdbcTemplate.update("""
                    INSERT INTO attendance_policy (id, absence_threshold, window_days, updated_by, updated_at)
                    VALUES (1, 3, 90, NULL, CURRENT_TIMESTAMP)
                    """);
        }
        jdbcTemplate.update(
                "UPDATE attendance_policy SET absence_threshold = 3, window_days = 90, updated_by = NULL WHERE id = 1");
        jdbcTemplate.update("DELETE FROM participant_fines");
        jdbcTemplate.update("DELETE FROM attendance_absences");
        jdbcTemplate.update("DELETE FROM event_attendance");
        jdbcTemplate.update("DELETE FROM event_registrations");
        jdbcTemplate.update("DELETE FROM event_cancellation_history");
        jdbcTemplate.update("DELETE FROM event_postponement_history");
        jdbcTemplate.update("DELETE FROM events");
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");
        participantChangeHistoryRepository.deleteAll();
        participantRepository.deleteAll();

        userPersistencePort.save(UserAccount.createManaged(
                OPERATOR_EMAIL,
                passwordEncoder.encode(OPERATOR_PASSWORD),
                Set.of(UserRole.OPERADOR),
                LocalDateTime.now(clock)));
        userPersistencePort.save(UserAccount.createManaged(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD),
                Set.of(UserRole.ADMINISTRADOR),
                LocalDateTime.now(clock)));
        operatorToken = login(OPERATOR_EMAIL, OPERATOR_PASSWORD);
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
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
    void invitadoPuedeInscribirseSoloComoSuParticipanteVinculado() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-07T09:00:00", "2027-05-07T12:00:00");
        UUID linkedParticipantId = createParticipant("10010", "linked@example.com");
        UUID otherParticipantId = createParticipant("10011", "other@example.com");
        String guestEmail = "self-registration@example.com";
        userPersistencePort.save(UserAccount.createManaged(
                guestEmail,
                passwordEncoder.encode("guest-password-123"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));
        UUID guestId = userPersistencePort.findByEmail(guestEmail).orElseThrow().getId();

        mockMvc.perform(post("/api/admin/users/{userId}/participant", guestId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantId":"%s"}
                                """.formatted(linkedParticipantId)))
                .andExpect(status().isOk());

        String guestToken = login(guestEmail, "guest-password-123");
        mockMvc.perform(post("/api/events/{eventId}/registrations/self", eventId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantId").value(linkedParticipantId.toString()));

        org.junit.jupiter.api.Assertions.assertEquals(1, activeCount(eventId));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND participant_id = ?",
                Integer.class,
                eventId,
                otherParticipantId));
    }

    @Test
    void invitadoSinParticipanteVinculadoNoPuedeInscribirse() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-08T09:00:00", "2027-05-08T12:00:00");
        String guestEmail = "unlinked-registration@example.com";
        userPersistencePort.save(UserAccount.createManaged(
                guestEmail,
                passwordEncoder.encode("guest-password-123"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));
        String guestToken = login(guestEmail, "guest-password-123");

        mockMvc.perform(post("/api/events/{eventId}/registrations/self", eventId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Participante no vinculado"));

        org.junit.jupiter.api.Assertions.assertEquals(0, activeCount(eventId));
    }

    @Test
    void operadorNoPuedeUsarLaRutaDeInscripcionPropiaDeInvitado() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-09T09:00:00", "2027-05-09T12:00:00");

        mockMvc.perform(post("/api/events/{eventId}/registrations/self", eventId)
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void registraCheckInQrParaInscripcionActivaEnEventoEnCurso() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-10T09:00:00", "2027-05-10T12:00:00");
        UUID participantId = createParticipant("10012", "qr-attendance@example.com");
        String qrToken = createRegistration(eventId, participantId)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        qrToken = objectMapper.readTree(qrToken).get("qrToken").asText();
        setEventInProgress(eventId);

        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken":"%s"}
                                """.formatted(qrToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.participantId").value(participantId.toString()))
                .andExpect(jsonPath("$.method").value("QR"))
                .andExpect(jsonPath("$.checkedInBy").isString())
                .andExpect(jsonPath("$.checkedInAt").isString());

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_attendance",
                Integer.class));
    }

    @Test
    void rechazaQrInexistenteYEventoQueNoEstaEnCurso() throws Exception {
        UUID publishedEventId = createPublishedEvent(10, "2027-05-11T09:00:00", "2027-05-11T12:00:00");
        UUID inProgressEventId = createInProgressEvent(10, "2027-05-12T09:00:00", "2027-05-12T12:00:00");

        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", inProgressEventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken":"not-a-valid-qr-token"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Código QR inválido"));

        UUID participantId = createParticipant("10013", "published-attendance@example.com");
        createRegistration(publishedEventId, participantId).andExpect(status().isCreated());
        String qrToken = jdbcTemplate.queryForObject(
                "SELECT qr_token FROM event_registrations WHERE event_id = ?",
                String.class,
                publishedEventId);
        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", publishedEventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken":"%s"}
                                """.formatted(qrToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El evento debe estar EN_CURSO para registrar asistencia."));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_attendance",
                Integer.class));
    }

    @Test
    void rechazaCheckInQrDuplicadoYDevuelveHoraAnterior() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-13T09:00:00", "2027-05-13T12:00:00");
        UUID participantId = createParticipant("10014", "duplicate-attendance@example.com");
        String response = createRegistration(eventId, participantId)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String qrToken = objectMapper.readTree(response).get("qrToken").asText();
        setEventInProgress(eventId);
        String checkIn = """
                {"qrToken":"%s"}
                """.formatted(qrToken);

        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Asistencia ya registrada"))
                .andExpect(jsonPath("$.checkedInAt").isString());
    }

    @Test
    void buscaInscritosPorDocumentoONombreYRegistraAsistenciaManual() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-14T09:00:00", "2027-05-14T12:00:00");
        UUID firstParticipantId = createParticipant("10015", "homonym1@example.com");
        UUID secondParticipantId = createParticipant("10016", "homonym2@example.com");
        createRegistration(eventId, firstParticipantId).andExpect(status().isCreated());
        createRegistration(eventId, secondParticipantId).andExpect(status().isCreated());
        jdbcTemplate.update(
                "UPDATE participants SET first_name = 'Alex', last_name = 'Gomez' WHERE id IN (?, ?)",
                firstParticipantId,
                secondParticipantId);
        setEventInProgress(eventId);

        mockMvc.perform(get("/api/events/{eventId}/attendance/registrations", eventId)
                        .header("Authorization", bearer())
                        .param("q", "Alex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].documentNumber").isString())
                .andExpect(jsonPath("$[1].documentNumber").isString());

        mockMvc.perform(post("/api/events/{eventId}/attendance/manual", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantId":"%s"}
                                """.formatted(firstParticipantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantId").value(firstParticipantId.toString()))
                .andExpect(jsonPath("$.method").value("MANUAL"))
                .andExpect(jsonPath("$.checkedInBy").isString());

        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = 'ATTENDANCE_MANUAL'",
                Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_attendance WHERE method = 'MANUAL'",
                Integer.class));
    }

    @Test
    void checkInManualSoloParaInscritoActivoEnEventoEnCurso() throws Exception {
        UUID eventId = createInProgressEvent(10, "2027-05-15T09:00:00", "2027-05-15T12:00:00");
        UUID participantId = createParticipant("10017", "manual-no-registration@example.com");

        mockMvc.perform(post("/api/events/{eventId}/attendance/manual", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantId":"%s"}
                                """.formatted(participantId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Inscripción activa no encontrada"));
    }

    @Test
    void invitadoNoPuedeRegistrarAsistencia() throws Exception {
        UUID eventId = createInProgressEvent(10, "2027-05-16T09:00:00", "2027-05-16T12:00:00");
        String guestEmail = "attendance-guest@example.com";
        userPersistencePort.save(UserAccount.createManaged(
                guestEmail,
                passwordEncoder.encode("guest-password-123"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));
        String guestToken = login(guestEmail, "guest-password-123");

        mockMvc.perform(get("/api/events/{eventId}/attendance/registrations", eventId)
                        .header("Authorization", "Bearer " + guestToken)
                        .param("q", "participant"))
                .andExpect(status().isForbidden());
    }

    @Test
    void politicaInicialUsaUmbralTresYVentanaDeNoventaDias() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-policy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.absenceThreshold").value(3))
                .andExpect(jsonPath("$.windowDays").value(90));
    }

    @Test
    void alSuperarUmbralSeGeneraMultaQueBloqueaInscripcionHastaResolucionAdmin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/admin/attendance-policy")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"absenceThreshold":1,"windowDays":90}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.absenceThreshold").value(1))
                .andExpect(jsonPath("$.windowDays").value(90));

        UUID participantId = createParticipant("10018", "absence@example.com");
        UUID firstEvent = createPublishedEvent(10, "2027-05-20T09:00:00", "2027-05-20T12:00:00");
        createRegistration(firstEvent, participantId).andExpect(status().isCreated());
        finishEvent(firstEvent);
        mockMvc.perform(get("/api/admin/fines")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        UUID secondEvent = createPublishedEvent(10, "2027-05-22T09:00:00", "2027-05-22T12:00:00");
        createRegistration(secondEvent, participantId).andExpect(status().isCreated());
        finishEvent(secondEvent);

        MvcResult finesResult = mockMvc.perform(get("/api/admin/fines")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].participantId").value(participantId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDIENTE"))
                .andReturn();
        UUID fineId = UUID.fromString(objectMapper.readTree(finesResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText());

        UUID futureEvent = createPublishedEvent(10, "2027-05-25T09:00:00", "2027-05-25T12:00:00");
        createRegistration(futureEvent, participantId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Inscripción bloqueada por multa pendiente"));

        mockMvc.perform(post("/api/admin/fines/{fineId}/resolution", fineId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESUELTA"))
                .andExpect(jsonPath("$.resolvedBy").isString());

        createRegistration(futureEvent, participantId).andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = 'FINE_RESOLVED'",
                Integer.class));
    }

    @Test
    void soloAdministradorPuedeConfigurarPoliticaYResolverMultas() throws Exception {
        UUID participantId = createParticipant("10019", "admin-only-fine@example.com");
        UUID eventId = createPublishedEvent(10, "2027-05-26T09:00:00", "2027-05-26T12:00:00");
        createRegistration(eventId, participantId).andExpect(status().isCreated());
        finishEvent(eventId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/admin/attendance-policy")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"absenceThreshold":2,"windowDays":30}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/fines/{fineId}/resolution", UUID.randomUUID())
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelacionLiberaCupoInactivaQrYRegistraAuditoria() throws Exception {
        UUID eventId = createPublishedEvent(1, "2027-05-26T09:00:00", "2027-05-26T12:00:00");
        UUID firstParticipant = createParticipant("10023", "cancel-first@example.com");
        UUID nextParticipant = createParticipant("10024", "cancel-next@example.com");
        MvcResult registrationResult = createRegistration(eventId, firstParticipant)
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode registration = objectMapper.readTree(registrationResult.getResponse().getContentAsString());
        UUID registrationId = UUID.fromString(registration.get("id").asText());
        String qrToken = registration.get("qrToken").asText();

        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, registrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"No podré asistir"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.reason").value("No podré asistir"));

        org.junit.jupiter.api.Assertions.assertEquals(0, activeCount(eventId));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_cancellation_history WHERE registration_id = ? AND reason = ?",
                Integer.class,
                registrationId,
                "No podré asistir"));
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_activity WHERE activity = 'REGISTRATION_CANCELLED'",
                Integer.class));
        createRegistration(eventId, nextParticipant).andExpect(status().isCreated());

        setEventInProgress(eventId);
        mockMvc.perform(post("/api/events/{eventId}/attendance/qr", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken":"%s"}
                                """.formatted(qrToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Código QR inválido"));
    }

    @Test
    void invitadoSoloPuedeCancelarInscripcionPropia() throws Exception {
        UUID eventId = createPublishedEvent(5, "2027-05-26T14:00:00", "2027-05-26T17:00:00");
        UUID ownParticipant = createParticipant("10025", "guest-own@example.com");
        UUID otherParticipant = createParticipant("10026", "guest-other@example.com");
        UUID ownRegistration = registrationId(createRegistration(eventId, ownParticipant));
        UUID otherRegistration = registrationId(createRegistration(eventId, otherParticipant));
        String guestEmail = "registration-guest@example.com";
        UserAccount guest = userPersistencePort.save(UserAccount.createManaged(
                guestEmail,
                passwordEncoder.encode("guest-password-123"),
                Set.of(UserRole.INVITADO),
                LocalDateTime.now(clock)));
        mockMvc.perform(post("/api/admin/users/{userId}/participant", guest.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantId":"%s"}
                                """.formatted(ownParticipant)))
                .andExpect(status().isOk());
        String guestToken = login(guestEmail, "guest-password-123");

        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, ownRegistration)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Cambio de planes"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, otherRegistration)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Intento de cancelar otra inscripción"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Cancelación no autorizada"));
    }

    @Test
    void rechazaCancelacionSinMotivoConAsistenciaOEventoCerrado() throws Exception {
        UUID eventId = createPublishedEvent(5, "2027-05-26T18:00:00", "2027-05-26T21:00:00");
        UUID participantId = createParticipant("10027", "cancel-invalid@example.com");
        UUID registrationId = registrationId(createRegistration(eventId, participantId));

        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, registrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"  "}
                                """))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(1, activeCount(eventId));

        setEventInProgress(eventId);
        mockMvc.perform(post("/api/events/{eventId}/attendance/manual", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantId":"%s"}
                                """.formatted(participantId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, registrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Ya asistió"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cancelación de inscripción no permitida"));

        UUID closedEventId = createPublishedEvent(5, "2027-05-27T09:00:00", "2027-05-27T12:00:00");
        UUID closedParticipant = createParticipant("10028", "cancel-closed@example.com");
        UUID closedRegistrationId = registrationId(createRegistration(closedEventId, closedParticipant));
        finishEvent(closedEventId);
        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                closedEventId, closedRegistrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Evento finalizado"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaCancelarInscripcionDeEventoCancelado() throws Exception {
        UUID eventId = createPublishedEvent(5, "2027-05-29T09:00:00", "2027-05-29T12:00:00");
        UUID participantId = createParticipant("10029", "cancel-event@example.com");
        UUID registrationId = registrationId(createRegistration(eventId, participantId));
        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Cambio de agenda"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, registrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Ya no aplica"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void finalizacionExcluyeInscripcionCanceladaYEsIdempotente() throws Exception {
        UUID eventId = createPublishedEvent(10, "2027-05-27T09:00:00", "2027-05-27T12:00:00");
        UUID activeParticipant = createParticipant("10020", "active-absence@example.com");
        UUID canceledParticipant = createParticipant("10021", "canceled-absence@example.com");
        createRegistration(eventId, activeParticipant).andExpect(status().isCreated());
        UUID canceledRegistrationId = registrationId(createRegistration(eventId, canceledParticipant));
        mockMvc.perform(post("/api/events/{eventId}/registrations/{registrationId}/cancellation",
                                eventId, canceledRegistrationId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Cancelación antes del evento"}
                                """))
                .andExpect(status().isOk());

        finishEvent(eventId);
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_absences WHERE event_id = ?",
                Integer.class,
                eventId));
        absenceService.processFinalizedEvent(eventId);
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance_absences WHERE event_id = ?",
                Integer.class,
                eventId));
    }

    @Test
    void inasistenciasFueraDelPeriodoNoSumanAlUmbral() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/admin/attendance-policy")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"absenceThreshold":1,"windowDays":30}
                                """))
                .andExpect(status().isOk());

        UUID participantId = createParticipant("10022", "expired-absence@example.com");
        UUID firstEvent = createPublishedEvent(10, "2027-05-28T09:00:00", "2027-05-28T12:00:00");
        createRegistration(firstEvent, participantId).andExpect(status().isCreated());
        finishEvent(firstEvent);
        jdbcTemplate.update(
                "UPDATE attendance_absences SET recorded_at = ? WHERE participant_id = ?",
                LocalDateTime.now(clock).minusDays(31),
                participantId);

        UUID secondEvent = createPublishedEvent(10, "2027-05-30T09:00:00", "2027-05-30T12:00:00");
        createRegistration(secondEvent, participantId).andExpect(status().isCreated());
        finishEvent(secondEvent);
        mockMvc.perform(get("/api/admin/fines")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        UUID thirdEvent = createPublishedEvent(10, "2027-06-01T09:00:00", "2027-06-01T12:00:00");
        createRegistration(thirdEvent, participantId).andExpect(status().isCreated());
        finishEvent(thirdEvent);
        mockMvc.perform(get("/api/admin/fines")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
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

    private UUID registrationId(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        MvcResult registrationResult = result.andExpect(status().isCreated()).andReturn();
        return UUID.fromString(objectMapper.readTree(registrationResult.getResponse().getContentAsString())
                .get("id").asText());
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

    private UUID createInProgressEvent(int capacity, String startAt, String endAt) throws Exception {
        UUID eventId = createPublishedEvent(capacity, startAt, endAt);
        setEventInProgress(eventId);
        return eventId;
    }

    private void setEventInProgress(UUID eventId) throws Exception {
        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"EN_CURSO"}
                                """))
                .andExpect(status().isOk());
    }

    private void finishEvent(UUID eventId) throws Exception {
        setEventInProgress(eventId);
        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FINALIZADO"}
                                """))
                .andExpect(status().isOk());
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
