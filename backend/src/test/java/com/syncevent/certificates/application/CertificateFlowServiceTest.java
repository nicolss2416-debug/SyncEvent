package com.syncevent.certificates.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncevent.attendance.domain.AttendancePersistencePort;
import com.syncevent.attendance.domain.CheckIn;
import com.syncevent.attendance.domain.CheckInMethod;
import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificatePersistencePort;
import com.syncevent.certificates.domain.CertificateStatus;
import com.syncevent.certificates.domain.CertificateVerificationStatus;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.users.application.AuthActivityRecorder;

@ExtendWith(MockitoExtension.class)
class CertificateFlowServiceTest {

    private static final UUID REGISTRATION_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String ACTOR_EMAIL = "admin@example.com";

    @Mock
    private RegistrationPersistencePort registrationPersistencePort;

    @Mock
    private EventPersistencePort eventPersistencePort;

    @Mock
    private ParticipantPersistencePort participantPersistencePort;

    @Mock
    private AttendancePersistencePort attendancePersistencePort;

    @Mock
    private CertificatePersistencePort certificatePersistencePort;

    @Mock
    private CertificatePdfRenderer pdfRenderer;

    @Mock
    private AuthActivityRecorder activityRecorder;

    private Clock clock;
    private CertificateIssueService issueService;
    private CertificateVerificationService verificationService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-05-01T10:15:30Z"), ZoneId.of("UTC"));
        issueService = new CertificateIssueService(
                registrationPersistencePort,
                eventPersistencePort,
                participantPersistencePort,
                attendancePersistencePort,
                certificatePersistencePort,
                pdfRenderer,
                activityRecorder,
                clock);
        verificationService = new CertificateVerificationService(
                certificatePersistencePort,
                registrationPersistencePort,
                eventPersistencePort,
                participantPersistencePort);
    }

    @Test
    void issueGeneratesCertificateWhenRegistrationIsEligible() {
        Registration registration = Registration.reconstitute(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                "token-123",
                ACTOR_ID,
                LocalDateTime.of(2026, 5, 1, 8, 0),
                com.syncevent.registrations.domain.RegistrationStatus.ACTIVA);
        Event event = new Event(
                EVENT_ID,
                "Evento finalizado",
                "Descripción",
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0),
                "Bogotá",
                null,
                50,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 3, 10, 8, 0),
                LocalDateTime.of(2026, 4, 2, 9, 0));
        Participant participant = Participant.reconstitute(
                PARTICIPANT_ID,
                "CC",
                "10101010",
                "Ana",
                "García",
                "ana@example.com",
                "3001234567",
                "system",
                LocalDateTime.of(2026, 4, 1, 7, 0),
                null,
                null);

        when(registrationPersistencePort.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(eventPersistencePort.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(participantPersistencePort.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(attendancePersistencePort.findByRegistrationId(REGISTRATION_ID))
                .thenReturn(Optional.of(new CheckIn(REGISTRATION_ID, LocalDateTime.of(2026, 4, 1, 9, 30), ACTOR_ID, CheckInMethod.QR)));
        when(certificatePersistencePort.findByRegistrationId(REGISTRATION_ID)).thenReturn(Optional.empty());
        when(pdfRenderer.render(any(Certificate.class), eq(event), eq(participant))).thenReturn(new byte[] {1, 2, 3, 4});
        when(certificatePersistencePort.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Certificate certificate = issueService.issue(REGISTRATION_ID, ACTOR_ID, ACTOR_EMAIL);

        assertNotNull(certificate);
        assertEquals(REGISTRATION_ID, certificate.registrationId());
        assertEquals(CertificateStatus.VIGENTE, certificate.status());
        assertNotNull(certificate.verificationCode());
        verify(certificatePersistencePort).save(any(Certificate.class));
    }

    @Test
    void issueRejectsRegistrationWithoutRecordedAttendance() {
        Registration registration = Registration.reconstitute(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                "token-123",
                ACTOR_ID,
                LocalDateTime.of(2026, 5, 1, 8, 0),
                com.syncevent.registrations.domain.RegistrationStatus.ACTIVA);
        Event event = new Event(
                EVENT_ID,
                "Evento finalizado",
                "Descripción",
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0),
                "Bogotá",
                null,
                50,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 3, 10, 8, 0),
                LocalDateTime.of(2026, 4, 2, 9, 0));
        Participant participant = Participant.reconstitute(
                PARTICIPANT_ID,
                "CC",
                "10101010",
                "Ana",
                "García",
                "ana@example.com",
                "3001234567",
                "system",
                LocalDateTime.of(2026, 4, 1, 7, 0),
                null,
                null);

        when(registrationPersistencePort.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(eventPersistencePort.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(participantPersistencePort.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(attendancePersistencePort.findByRegistrationId(REGISTRATION_ID)).thenReturn(Optional.empty());

        assertThrows(CertificateExceptions.CertificateNotEligibleException.class,
                () -> issueService.issue(REGISTRATION_ID, ACTOR_ID, ACTOR_EMAIL));
    }

    @Test
    void verifyReturnsNotFoundWhenCodeDoesNotExist() {
        when(certificatePersistencePort.findByVerificationCode("CERT-MISSING-404")).thenReturn(Optional.empty());

        var result = verificationService.verify("CERT-MISSING-404");

        assertEquals(CertificateVerificationStatus.NOT_FOUND, result.status());
        assertEquals("No existe un certificado con ese código.", result.message());
    }

    @Test
    void verifyReturnsCounterfeitWhenHashDoesNotMatch() {
        Registration registration = Registration.reconstitute(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                "token-123",
                ACTOR_ID,
                LocalDateTime.of(2026, 5, 1, 8, 0),
                com.syncevent.registrations.domain.RegistrationStatus.ACTIVA);
        Event event = new Event(
                EVENT_ID,
                "Evento de prueba",
                "Descripción",
                TipoEvento.TALLER,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 4, 12, 9, 0),
                LocalDateTime.of(2026, 4, 12, 12, 0),
                "Bogotá",
                null,
                60,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 4, 9, 8, 0),
                LocalDateTime.of(2026, 4, 13, 9, 0));
        Participant participant = Participant.reconstitute(
                PARTICIPANT_ID,
                "CC",
                "10101010",
                "Ana",
                "García",
                "ana@example.com",
                "3001234567",
                "system",
                LocalDateTime.of(2026, 4, 12, 7, 0),
                null,
                null);

        Certificate certificate = Certificate.issue(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                ACTOR_ID,
                "Ana García",
                "Evento de prueba",
                event.getStartAt(),
                event.getEndAt(),
                "CERT-TAMPER-789");
        Certificate tampered = certificate.withHashes("sha256-falsificado", "pdf-hash");

        when(certificatePersistencePort.findByVerificationCode(tampered.verificationCode())).thenReturn(Optional.of(tampered));
        when(registrationPersistencePort.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(eventPersistencePort.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(participantPersistencePort.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));

        var result = verificationService.verify(tampered.verificationCode());

        assertEquals(CertificateVerificationStatus.COUNTERFEIT, result.status());
        assertEquals("El certificado no coincide con su hash firmado.", result.message());
    }

    @Test
    void verifyReturnsCounterfeitWhenRegistrationContextIsMissing() {
        Certificate certificate = Certificate.issue(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                ACTOR_ID,
                "Ana García",
                "Evento de prueba",
                LocalDateTime.of(2026, 4, 12, 9, 0),
                LocalDateTime.of(2026, 4, 12, 12, 0),
                "CERT-INVALID-CONTEXT-321");
        Certificate intact = certificate.withHashes(Certificate.sha256(certificate.canonicalPayload()), "pdf-hash");

        when(certificatePersistencePort.findByVerificationCode(intact.verificationCode())).thenReturn(Optional.of(intact));
        when(registrationPersistencePort.findById(REGISTRATION_ID)).thenReturn(Optional.empty());

        var result = verificationService.verify(intact.verificationCode());

        assertEquals(CertificateVerificationStatus.COUNTERFEIT, result.status());
        assertEquals("La inscripción asociada ya no es válida.", result.message());
    }

    @Test
    void verifyReturnsValidForIntactCertificateAndRevokedForRevokedOne() {
        Registration registration = Registration.reconstitute(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                "token-123",
                ACTOR_ID,
                LocalDateTime.of(2026, 5, 1, 8, 0),
                com.syncevent.registrations.domain.RegistrationStatus.ACTIVA);
        Event event = new Event(
                EVENT_ID,
                "Evento de prueba",
                "Descripción",
                TipoEvento.TALLER,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2026, 4, 12, 9, 0),
                LocalDateTime.of(2026, 4, 12, 12, 0),
                "Bogotá",
                null,
                60,
                BigDecimal.valueOf(3),
                EstadoEvento.FINALIZADO,
                LocalDateTime.of(2026, 4, 9, 8, 0),
                LocalDateTime.of(2026, 4, 13, 9, 0));
        Participant participant = Participant.reconstitute(
                PARTICIPANT_ID,
                "CC",
                "10101010",
                "Ana",
                "García",
                "ana@example.com",
                "3001234567",
                "system",
                LocalDateTime.of(2026, 4, 12, 7, 0),
                null,
                null);

        Certificate certificate = Certificate.issue(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                ACTOR_ID,
                "Ana García",
                "Evento de prueba",
                event.getStartAt(),
                event.getEndAt(),
                "CERT-VALID-123");
        Certificate intact = certificate.withHashes(Certificate.sha256(certificate.canonicalPayload()), "pdf-hash");

        when(certificatePersistencePort.findByVerificationCode(intact.verificationCode())).thenReturn(Optional.of(intact));
        when(registrationPersistencePort.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
        when(eventPersistencePort.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(participantPersistencePort.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));

        var valid = verificationService.verify(intact.verificationCode());

        assertEquals(CertificateVerificationStatus.VALID, valid.status());

        Certificate revokedSeed = Certificate.issue(
                REGISTRATION_ID,
                EVENT_ID,
                PARTICIPANT_ID,
                ACTOR_ID,
                "Ana García",
                "Evento de prueba",
                event.getStartAt(),
                event.getEndAt(),
                "CERT-REVOKED-456");
        Certificate revoked = revokedSeed.withHashes(
                Certificate.sha256(revokedSeed.canonicalPayload()),
                "revoked-pdf-hash").revoke("Fraud detection", LocalDateTime.of(2026, 4, 14, 10, 0));
        when(certificatePersistencePort.findByVerificationCode("CERT-REVOKED-456")).thenReturn(Optional.of(revoked));

        var result = verificationService.verify("CERT-REVOKED-456");

        assertEquals(CertificateVerificationStatus.REVOKED, result.status());
        assertEquals("Fraud detection", revoked.revocationReason());
    }
}
