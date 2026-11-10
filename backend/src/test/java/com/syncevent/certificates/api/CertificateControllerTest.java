package com.syncevent.certificates.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificatePersistencePort;
import com.syncevent.certificates.domain.CertificateStatus;
import com.syncevent.events.domain.EstadoEvento;
import com.syncevent.events.domain.Event;
import com.syncevent.events.domain.Modalidad;
import com.syncevent.events.domain.TipoEvento;
import com.syncevent.events.infrastructure.EventEntity;
import com.syncevent.events.infrastructure.SpringDataEventRepository;
import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.infrastructure.ParticipantEntity;
import com.syncevent.participants.infrastructure.SpringDataParticipantRepository;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CertificateControllerTest {

    private static final String ADMIN_EMAIL = "certificate-admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserPersistencePort userPersistencePort;

    @Autowired
    private CertificatePersistencePort certificatePersistencePort;

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Autowired
    private SpringDataParticipantRepository participantRepository;

    @Autowired
    private Clock clock;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM certificate_documents");
        jdbcTemplate.update("DELETE FROM event_registrations");
        jdbcTemplate.update("UPDATE user_accounts SET participant_id = NULL");
        jdbcTemplate.update("DELETE FROM participants");
        jdbcTemplate.update("DELETE FROM auth_activity");
        jdbcTemplate.update("DELETE FROM auth_sessions");
        jdbcTemplate.update("DELETE FROM user_account_roles");
        jdbcTemplate.update("DELETE FROM user_accounts");
        jdbcTemplate.update("DELETE FROM events");

        eventRepository.deleteAll();
        participantRepository.deleteAll();

        userPersistencePort.save(UserAccount.createManaged(
                ADMIN_EMAIL,
                "encoded-password",
                Set.of(UserRole.ADMINISTRADOR),
                LocalDateTime.now(clock)));
    }

    @Test
    void publicVerificationEndpointReturnsValidStatusForExistingCertificate() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        String verificationCode = "CERT-VALID-001";

        Event event = new Event(
                eventId,
                "Curso de seguridad",
                "Curso de seguridad",
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 12, 0),
                "Bogotá",
                null,
                30,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 4, 8, 9, 0),
                LocalDateTime.of(2026, 4, 10, 13, 0));
        eventRepository.save(EventEntity.fromDomain(event));

        jdbcTemplate.update(
                "INSERT INTO participants (id, document_type, document_number, first_name, last_name, email, phone, created_by, created_at, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                "CC",
                "10101010",
                "Ana",
                "García",
                "ana@example.com",
                "3001112233",
                "system",
                LocalDateTime.of(2026, 4, 7, 8, 0),
                null,
                null);

        UUID adminId = userPersistencePort.findByEmail(ADMIN_EMAIL).orElseThrow().getId();

        jdbcTemplate.update(
                "INSERT INTO event_registrations (id, event_id, participant_id, qr_token, created_by, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                registrationId,
                eventId,
                participantId,
                "qr-token-123",
                adminId,
                LocalDateTime.of(2026, 4, 8, 8, 0),
                "ACTIVA");

        Certificate certificate = Certificate.issue(
                registrationId,
                eventId,
                participantId,
                adminId,
                "Ana García",
                "Curso de seguridad",
                event.getStartAt(),
                event.getEndAt(),
                verificationCode);
        Certificate persisted = certificate.withHashes(
                Certificate.sha256(certificate.canonicalPayload()),
                "hash-pdf-001");

        certificatePersistencePort.save(persisted);

        mockMvc.perform(get("/api/public/certificates/{verificationCode}", verificationCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.verificationCode").value(verificationCode))
                .andExpect(jsonPath("$.participantName").value("Ana García"))
                .andExpect(jsonPath("$.eventName").value("Curso de seguridad"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMINISTRADOR")
    void downloadCertificateEndpointReturnsPdf() throws Exception {
        UUID certificateId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID adminId = userPersistencePort.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        String verificationCode = "CERT-DOWNLOAD-003";

        Event event = new Event(
                eventId,
                "Seminario de auditoría",
                "Seminario",
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 5, 15, 9, 0),
                LocalDateTime.of(2026, 5, 15, 12, 0),
                "Bogotá",
                null,
                25,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 5, 10, 8, 0),
                LocalDateTime.of(2026, 5, 16, 8, 0));
        eventRepository.save(EventEntity.fromDomain(event));

        jdbcTemplate.update(
                "INSERT INTO participants (id, document_type, document_number, first_name, last_name, email, phone, created_by, created_at, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                "CC",
                "30303030",
                "María",
                "López",
                "maria@example.com",
                "3223334444",
                "system",
                LocalDateTime.of(2026, 5, 11, 7, 0),
                null,
                null);

        jdbcTemplate.update(
                "INSERT INTO event_registrations (id, event_id, participant_id, qr_token, created_by, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                registrationId,
                eventId,
                participantId,
                "qr-token-download",
                adminId,
                LocalDateTime.of(2026, 5, 11, 8, 0),
                "ACTIVA");

        Certificate pendingCertificate = Certificate.issue(
                registrationId,
                eventId,
                participantId,
                adminId,
                "María López",
                "CC",
                "30303030",
                "Seminario de auditoría",
                event.getStartAt(),
                event.getEndAt(),
                verificationCode);
        Certificate certificate = pendingCertificate.withHashes(
                Certificate.sha256(pendingCertificate.canonicalPayload()), "pdf-hash-download");

        certificateId = certificatePersistencePort.save(certificate).id();

        mockMvc.perform(get("/api/certificates/{certificateId}/download", certificateId)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isForbidden());

        UserAccount account = userPersistencePort.findByEmail(ADMIN_EMAIL).orElseThrow();
        userPersistencePort.save(account.linkParticipant(participantId));

        jdbcTemplate.update("UPDATE participants SET first_name = ?, document_number = ? WHERE id = ?",
                "Nombre Actualizado", "99999999", participantId);
        jdbcTemplate.update("UPDATE events SET name = ? WHERE id = ?", "Evento Actualizado", eventId);

        mockMvc.perform(get("/api/certificates/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(certificateId.toString()))
                .andExpect(jsonPath("$[0].participantId").value(participantId.toString()));

        var pdfResponse = mockMvc.perform(get("/api/certificates/{certificateId}/download", certificateId)
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentType())
                        .contains("application/pdf"))
                .andReturn();
        try (var pdf = Loader.loadPDF(pdfResponse.getResponse().getContentAsByteArray())) {
            String content = new PDFTextStripper().getText(pdf);
            org.assertj.core.api.Assertions.assertThat(content)
                    .contains("María López", "30303030", "Seminario de auditoría")
                    .doesNotContain("Nombre Actualizado", "99999999", "Evento Actualizado");
        }
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMINISTRADOR")
    void revokeCertificateEndpointMarksItRevoked() throws Exception {
        UUID certificateId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID registrationId = UUID.randomUUID();
        UUID adminId = userPersistencePort.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        String verificationCode = "CERT-REVOKE-002";

        Event event = new Event(
                eventId,
                "Taller de pruebas",
                "Taller",
                TipoEvento.TALLER,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 12, 0),
                "Bogotá",
                null,
                40,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 3, 8, 7, 0),
                LocalDateTime.of(2026, 3, 12, 8, 0));
        eventRepository.save(EventEntity.fromDomain(event));

        jdbcTemplate.update(
                "INSERT INTO participants (id, document_type, document_number, first_name, last_name, email, phone, created_by, created_at, updated_by, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                "CC",
                "20202020",
                "Luis",
                "Pérez",
                "luis@example.com",
                "3112223344",
                "system",
                LocalDateTime.of(2026, 3, 5, 7, 0),
                null,
                null);

        jdbcTemplate.update(
                "INSERT INTO event_registrations (id, event_id, participant_id, qr_token, created_by, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                registrationId,
                eventId,
                participantId,
                "qr-token-revoke",
                adminId,
                LocalDateTime.of(2026, 3, 5, 8, 0),
                "ACTIVA");

        Certificate baseCertificate = Certificate.issue(
                registrationId,
                eventId,
                participantId,
                adminId,
                "Luis Pérez",
                "Taller de pruebas",
                event.getStartAt(),
                event.getEndAt(),
                verificationCode);
        Certificate persisted = baseCertificate.withHashes(
                Certificate.sha256(baseCertificate.canonicalPayload()),
                "hash-pdf-revoke");

        certificateId = certificatePersistencePort.save(persisted).id();

        mockMvc.perform(post("/api/certificates/{certificateId}/revoke", certificateId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Revocado por prueba de integración\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOCADO"))
                .andExpect(jsonPath("$.verificationCode").value(verificationCode));
    }
}
