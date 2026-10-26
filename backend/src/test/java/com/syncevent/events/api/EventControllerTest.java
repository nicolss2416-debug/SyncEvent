package com.syncevent.events.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import com.syncevent.events.infrastructure.persistence.SpringDataEventRepository;
import com.syncevent.events.infrastructure.persistence.EventCancellationHistoryRepository;
import com.syncevent.events.infrastructure.persistence.EventPostponementHistoryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Autowired
    private EventCancellationHistoryRepository cancellationHistoryRepository;

    @Autowired
    private EventPostponementHistoryRepository postponementHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEvents() {
        jdbcTemplate.update("DELETE FROM event_monitor_assignments");
        postponementHistoryRepository.deleteAll();
        cancellationHistoryRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void creaEventoYDevuelve201() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede central", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Capacitación Java"))
                .andExpect(jsonPath("$.status").value("BORRADOR"));

        org.junit.jupiter.api.Assertions.assertEquals(1, eventRepository.count());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaDatosInvalidosCon400() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": " ",
                          "type": "CAPACITACION",
                          "modality": "PRESENCIAL",
                          "startAt": "2027-03-01T09:00:00",
                          "endAt": "2027-03-01T12:00:00",
                          "location": "Sede central",
                          "capacity": 30,
                          "durationHours": 3
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaCruceDeHorarioEnElMismoLugarCon409() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede central", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede central", "2027-03-01T11:00:00", "2027-03-01T13:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de horario"));

        org.junit.jupiter.api.Assertions.assertEquals(1, eventRepository.count());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void permiteCruceDeHorarioEnLugaresDiferentes() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede norte", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede sur", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isCreated());

        org.junit.jupiter.api.Assertions.assertEquals(2, eventRepository.count());
    }

    @Test
    @WithMockUser(roles = "INVITADO")
    void deniegaCreacionAInvitado() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede central", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void listaEventosPaginadosYFiltraTipo() throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent("Sede norte", "2027-03-01T09:00:00", "2027-03-01T12:00:00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Taller de redes",
                          "type": "TALLER",
                          "modality": "PRESENCIAL",
                          "startAt": "2027-04-01T09:00:00",
                          "endAt": "2027-04-01T12:00:00",
                          "location": "Sede sur",
                          "capacity": 20,
                          "durationHours": 3
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events")
                .param("type", "CAPACITACION")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CAPACITACION"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(roles = "INVITADO")
    void invitadoSoloVeEventosPublicados() throws Exception {
        insertEvent("Evento borrador", "BORRADOR", "2027-03-01T09:00:00");
        insertEvent("Evento publicado", "PUBLICADO", "2027-03-02T09:00:00");
        insertEvent("Evento en curso", "EN_CURSO", "2027-03-03T09:00:00");

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PUBLICADO"));
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorVeEventosPublicadosYEnCurso() throws Exception {
        insertEvent("Evento borrador", "BORRADOR", "2027-03-01T09:00:00");
        insertEvent("Evento publicado", "PUBLICADO", "2027-03-02T09:00:00");
        insertEvent("Evento en curso", "EN_CURSO", "2027-03-03T09:00:00");

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value("PUBLICADO"))
                .andExpect(jsonPath("$.content[1].status").value("EN_CURSO"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaRangoDeFechasInvertido() throws Exception {
        mockMvc.perform(get("/api/events")
                .param("from", "2027-04-01")
                .param("to", "2027-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "La fecha final no puede ser anterior a la fecha inicial."));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void permiteAvanzarEstadosEnOrden() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        for (String nextStatus : new String[] { "PUBLICADO", "EN_CURSO", "FINALIZADO" }) {
            mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"status":"%s"}
                            """.formatted(nextStatus)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(nextStatus));
        }
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaSaltarEstadoCon409() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"EN_CURSO"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cambio de estado no permitido"))
                .andExpect(jsonPath("$.detail").value(
                        "Transición no permitida desde BORRADOR. Estado permitido: PUBLICADO."));

        org.junit.jupiter.api.Assertions.assertEquals(
                "BORRADOR",
                eventRepository.findById(eventId).orElseThrow().toDomain().getStatus().name());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaCambiarEstadoFinalCon409() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();
        jdbcTemplate.update("UPDATE events SET status = 'FINALIZADO' WHERE id = ?", eventId);

        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PUBLICADO"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cambio de estado no permitido"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void eventoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(patch("/api/events/{eventId}/status", java.util.UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PUBLICADO"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Evento no encontrado"));
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorNoPuedeCambiarEstado() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PUBLICADO"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void cancelaEventoConMotivoYConservaHistorial() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"No se cuenta con el espacio requerido"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM event_cancellation_history WHERE event_id = ? AND previous_status = 'BORRADOR' AND reason = ?",
                        Integer.class,
                        eventId,
                        "No se cuenta con el espacio requerido"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void permiteCancelarEventoPublicadoYRegistraEstadoAnterior() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();
        jdbcTemplate.update("UPDATE events SET status = 'PUBLICADO' WHERE id = ?", eventId);

        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"Cambio operativo"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        org.junit.jupiter.api.Assertions.assertEquals(
                "PUBLICADO",
                jdbcTemplate.queryForObject(
                        "SELECT previous_status FROM event_cancellation_history WHERE event_id = ?",
                        String.class,
                        eventId));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaCancelarEventoFinalizado() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();
        jdbcTemplate.update("UPDATE events SET status = 'FINALIZADO' WHERE id = ?", eventId);

        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"Motivo"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cancelación no permitida"));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_cancellation_history WHERE event_id = ?",
                Integer.class,
                eventId));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaCancelacionSinMotivo() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":" "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());

        org.junit.jupiter.api.Assertions.assertEquals(
                "BORRADOR",
                eventRepository.findById(eventId).orElseThrow().toDomain().getStatus().name());
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorNoPuedeCancelarEvento() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/events/{eventId}/cancellation", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"Motivo"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void aplazaEventoPublicadoConservandoMotivoEHistorial() throws Exception {
        java.util.UUID eventId = createAndPublishEvent();

        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        "Cambio de disponibilidad de la sede",
                        "2027-03-05T09:00:00",
                        "2027-03-05T12:00:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APLAZADO"))
                .andExpect(jsonPath("$.startAt").value("2027-03-05T09:00:00"));

        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM event_postponement_history WHERE event_id = ? AND action = 'SOLICITADO' AND reason = ?",
                        Integer.class,
                        eventId,
                        "Cambio de disponibilidad de la sede"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void soloPermiteAplazarEventosPublicados() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        "Cambio de sede",
                        "2027-03-05T09:00:00",
                        "2027-03-05T12:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Aplazamiento no permitido"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaAplazamientoSinMotivo() throws Exception {
        java.util.UUID eventId = createAndPublishEvent();

        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        " ",
                        "2027-03-05T09:00:00",
                        "2027-03-05T12:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void noAplazaSiLaNuevaFechaTieneConflicto() throws Exception {
        java.util.UUID eventId = createAndPublishEvent();
        createEventAt("Sede central", "2027-03-01T12:00:00", "2027-03-01T15:00:00");

        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        "Cambio de sede",
                        "2027-03-01T11:00:00",
                        "2027-03-01T14:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de horario"));

        org.junit.jupiter.api.Assertions.assertEquals(
                "PUBLICADO",
                eventRepository.findById(eventId).orElseThrow().toDomain().getStatus().name());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void confirmaFechaYRegresaEventoAPublicado() throws Exception {
        java.util.UUID eventId = createAndPublishEvent();
        requestPostponement(eventId);

        mockMvc.perform(post("/api/events/{eventId}/postponements/confirmation", eventId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLICADO"))
                .andExpect(jsonPath("$.startAt").value("2027-03-05T09:00:00"));

        org.junit.jupiter.api.Assertions.assertEquals(
                2,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM event_postponement_history WHERE event_id = ?",
                        Integer.class,
                        eventId));
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorNoPuedeAplazarEvento() throws Exception {
        java.util.UUID eventId = createAndPublishEvent();

        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        "Cambio",
                        "2027-03-05T09:00:00",
                        "2027-03-05T12:00:00")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void editaEventoYConservaEstado() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/events/{eventId}", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Capacitación Java avanzada", "Sede central",
                        "2027-03-01T09:00:00", "2027-03-01T12:00:00", 35)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Capacitación Java avanzada"))
                .andExpect(jsonPath("$.status").value("BORRADOR"))
                .andExpect(jsonPath("$.capacity").value(35));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaEdicionDeEventoFinalizado() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();
        jdbcTemplate.update("UPDATE events SET status = 'FINALIZADO' WHERE id = ?", eventId);

        mockMvc.perform(put("/api/events/{eventId}", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Nombre actualizado", "Sede central",
                        "2027-03-02T09:00:00", "2027-03-02T12:00:00", 25)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Edición no permitida"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaEdicionConConflictoDeLugarYHorario() throws Exception {
        createEvent("Sede central");
        createEvent("Sede norte");
        java.util.UUID eventId = eventRepository.findAll().stream()
                .filter(event -> "Sede norte".equals(event.toDomain().getLocation()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/events/{eventId}", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Nombre actualizado", "Sede central",
                        "2027-03-01T09:00:00", "2027-03-01T12:00:00", 35)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflicto de horario"));

        org.junit.jupiter.api.Assertions.assertEquals(
                "Sede norte",
                eventRepository.findById(eventId).orElseThrow().toDomain().getLocation());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void eventoInexistenteParaEdicionDevuelve404() throws Exception {
        mockMvc.perform(put("/api/events/{eventId}", java.util.UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Nombre actualizado", "Sede central",
                        "2027-03-02T09:00:00", "2027-03-02T12:00:00", 25)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void noPermiteCambiosDeFechaSinFlujoDeConfirmacionYNotificacion() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/events/{eventId}", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Nombre actualizado", "Sede central",
                        "2027-03-02T09:00:00", "2027-03-02T12:00:00", 35)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Edición no permitida"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void permiteReducirAforoCuandoNoHayInscripcionesActivas() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/events/{eventId}", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateEvent("Nombre actualizado", "Sede central",
                        "2027-03-01T09:00:00", "2027-03-01T12:00:00", 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(20));
    }

    private void createEvent(String location) throws Exception {
        createEventAt(location, "2027-03-01T09:00:00", "2027-03-01T12:00:00");
    }

    private void createEventAt(String location, String startAt, String endAt) throws Exception {
        mockMvc.perform(post("/api/events")
                .with(csrf())
                .with(user("operator").roles("OPERADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validEvent(location, startAt, endAt)))
                .andExpect(status().isCreated());
    }

    private java.util.UUID createAndPublishEvent() throws Exception {
        createEvent("Sede central");
        java.util.UUID eventId = eventRepository.findAll().get(0).getId();
        mockMvc.perform(patch("/api/events/{eventId}/status", eventId)
                .with(csrf())
                .with(user("operator").roles("OPERADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PUBLICADO"}
                        """))
                .andExpect(status().isOk());
        return eventId;
    }

    private void requestPostponement(java.util.UUID eventId) throws Exception {
        mockMvc.perform(post("/api/events/{eventId}/postponements", eventId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(postponementRequest(
                        "Cambio de disponibilidad de la sede",
                        "2027-03-05T09:00:00",
                        "2027-03-05T12:00:00")))
                .andExpect(status().isOk());
    }

    private String postponementRequest(String reason, String newStartAt, String newEndAt) {
        return """
                {
                  "reason": "%s",
                  "newStartAt": "%s",
                  "newEndAt": "%s"
                }
                """.formatted(reason, newStartAt, newEndAt);
    }

    private void insertEvent(String name, String status, String startAt) {
        jdbcTemplate.update("""
                INSERT INTO events (
                    id, name, type, modality, start_at, end_at, location,
                    capacity, duration_hours, status, created_at, updated_at
                ) VALUES (?, ?, 'CAPACITACION', 'PRESENCIAL', ?, ?, 'Sede central',
                    30, 3.00, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                java.util.UUID.randomUUID(),
                name,
                java.time.LocalDateTime.parse(startAt),
                java.time.LocalDateTime.parse(startAt).plusHours(3),
                status);
    }

    private String validEvent(String location, String startAt, String endAt) {
        return """
                {
                  "name": "Capacitación Java",
                  "description": "Introducción a Java",
                  "type": "CAPACITACION",
                  "modality": "PRESENCIAL",
                  "startAt": "%s",
                  "endAt": "%s",
                  "location": "%s",
                  "capacity": 30,
                  "durationHours": 3.00
                }
                """.formatted(startAt, endAt, location);
    }

    private String updateEvent(String name, String location, String startAt, String endAt, int capacity) {
        return """
                {
                  "name": "%s",
                  "description": "Descripción actualizada",
                  "type": "CAPACITACION",
                  "modality": "PRESENCIAL",
                  "startAt": "%s",
                  "endAt": "%s",
                  "location": "%s",
                  "capacity": %d,
                  "durationHours": 3
                }
                """.formatted(name, startAt, endAt, location, capacity);
    }
}
