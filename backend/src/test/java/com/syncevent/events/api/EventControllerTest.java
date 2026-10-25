package com.syncevent.events.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEvents() {
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
}
