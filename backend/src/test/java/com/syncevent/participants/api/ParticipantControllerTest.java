package com.syncevent.participants.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.syncevent.participants.infrastructure.SpringDataParticipantRepository;
import com.syncevent.participants.infrastructure.ParticipantChangeHistoryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataParticipantRepository participantRepository;

    @Autowired
    private ParticipantChangeHistoryRepository historyRepository;

    @BeforeEach
    void clearParticipants() {
        historyRepository.deleteAll();
        participantRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "operator-1", roles = "OPERADOR")
    void registraParticipanteNormalizaDatosYGuardaActor() throws Exception {
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant(" cc ", " ab-123 ", "  jUAN   cARLOS ", " pérez   gÓmez ")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.documentNumber").value("AB-123"))
                .andExpect(jsonPath("$.firstName").value("Juan Carlos"))
                .andExpect(jsonPath("$.lastName").value("Pérez Gómez"))
                .andExpect(jsonPath("$.createdBy").value("operator-1"))
                .andExpect(jsonPath("$.createdAt").exists());

        org.junit.jupiter.api.Assertions.assertEquals(1, participantRepository.count());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaDocumentoDuplicadoEIndicaParticipanteExistente() throws Exception {
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("CC", "12345", "Ana", "López")))
                .andExpect(status().isCreated());

        String existingId = participantRepository.findAll().get(0).getId().toString();
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("cc", " 12345 ", "Otro", "Nombre")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Participante ya registrado"))
                .andExpect(jsonPath("$.existingParticipantId").value(existingId));

        org.junit.jupiter.api.Assertions.assertEquals(1, participantRepository.count());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void validaCamposObligatoriosYCorreo() throws Exception {
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "documentType": "CC",
                          "documentNumber": "12345",
                          "firstName": " ",
                          "lastName": "López",
                          "email": "no-es-correo",
                          "phone": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists());

        org.junit.jupiter.api.Assertions.assertEquals(0, participantRepository.count());
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorNoPuedeRegistrarParticipantes() throws Exception {
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("CC", "12345", "Ana", "López")))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(0, participantRepository.count());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void buscaParticipantesPorDocumentoNombreYCorreoEnPaginas() throws Exception {
        registerParticipant("CC", "987654", "María Fernanda", "Gómez");
        registerParticipant("CE", "ABC-99", "Carlos", "Pérez");

        mockMvc.perform(get("/api/participants").param("q", "María"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("María Fernanda"));

        mockMvc.perform(get("/api/participants").param("q", "ABC").param("size", "1").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].documentNumber").value("ABC-99"));

        mockMvc.perform(get("/api/participants").param("q", "example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/participants").param("q", "example.com")
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Carlos"));
    }

    @Test
    @WithMockUser(roles = "MONITOR")
    void monitorPuedeConsultarFichaPeroNoEditar() throws Exception {
        registerParticipant("CC", "112233", "Ana", "López");
        java.util.UUID participantId = participantRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/participants/{participantId}", participantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ana"));

        mockMvc.perform(put("/api/participants/{participantId}", participantId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("CC", "112233", "Ana", "Pérez")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/participants/{participantId}", participantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("López"));
    }

    @Test
    @WithMockUser(username = "operator-2", roles = "OPERADOR")
    void actualizaParticipanteYGuardaHuellaDeCorreccion() throws Exception {
        registerParticipant("CC", "112233", "ANA", "López");
        java.util.UUID participantId = participantRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/participants/{participantId}", participantId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("cc", "223344", "  aNA  mARÍA ", " pérez ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("223344"))
                .andExpect(jsonPath("$.firstName").value("Ana María"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.createdBy").value("operator"))
                .andExpect(jsonPath("$.updatedBy").value("operator-2"))
                .andExpect(jsonPath("$.updatedAt").exists());

        org.junit.jupiter.api.Assertions.assertEquals(1, historyRepository.count());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void impideCambiarDocumentoPorUnoUsadoPorOtroParticipante() throws Exception {
        registerParticipant("CC", "11111", "Ana", "López");
        registerParticipant("CC", "22222", "Luis", "Pérez");
        java.util.UUID participantId = participantRepository.findByDocumentTypeAndDocumentNumber("CC", "11111")
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/participants/{participantId}", participantId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant("CC", "22222", "Ana", "López")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Participante ya registrado"));

        org.junit.jupiter.api.Assertions.assertEquals(0, historyRepository.count());
        mockMvc.perform(get("/api/participants/{participantId}", participantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("11111"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void buscarSinCoincidenciasDevuelvePaginaVacia() throws Exception {
        mockMvc.perform(get("/api/participants").param("q", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void fichaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/participants/{participantId}", java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Participante no encontrado"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void rechazaCorreccionConCamposInvalidos() throws Exception {
        registerParticipant("CC", "112233", "Ana", "López");
        java.util.UUID participantId = participantRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/participants/{participantId}", participantId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "documentType": "CC",
                          "documentNumber": "112233",
                          "firstName": " ",
                          "lastName": "López",
                          "email": "no-es-correo",
                          "phone": "3001234567"
                        }
                        """))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, historyRepository.count());
    }

    private void registerParticipant(
            String documentType, String documentNumber, String firstName, String lastName) throws Exception {
        mockMvc.perform(post("/api/participants")
                .with(csrf())
                .with(user("operator").roles("OPERADOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validParticipant(documentType, documentNumber, firstName, lastName)))
                .andExpect(status().isCreated());
    }

    private String validParticipant(
            String documentType, String documentNumber, String firstName, String lastName) {
        return """
                {
                  "documentType": "%s",
                  "documentNumber": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "persona@example.com",
                  "phone": "3001234567"
                }
                """.formatted(documentType, documentNumber, firstName, lastName);
    }
}
