package com.syncevent.certificates.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.attendance.domain.AttendancePersistencePort;
import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificatePersistencePort;
import com.syncevent.certificates.domain.CertificateStatus;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.application.ParticipantNotFoundException;
import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.users.application.AuthActivityRecorder;

@Service
public class CertificateIssueService {

    private final RegistrationPersistencePort registrationPersistencePort;
    private final EventPersistencePort eventPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final AttendancePersistencePort attendancePersistencePort;
    private final CertificatePersistencePort certificatePersistencePort;
    private final CertificatePdfRenderer pdfRenderer;
    private final AuthActivityRecorder activityRecorder;
    private final Clock clock;

    public CertificateIssueService(
            RegistrationPersistencePort registrationPersistencePort,
            EventPersistencePort eventPersistencePort,
            ParticipantPersistencePort participantPersistencePort,
            AttendancePersistencePort attendancePersistencePort,
            CertificatePersistencePort certificatePersistencePort,
            CertificatePdfRenderer pdfRenderer,
            AuthActivityRecorder activityRecorder,
            Clock clock) {
        this.registrationPersistencePort = registrationPersistencePort;
        this.eventPersistencePort = eventPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.attendancePersistencePort = attendancePersistencePort;
        this.certificatePersistencePort = certificatePersistencePort;
        this.pdfRenderer = pdfRenderer;
        this.activityRecorder = activityRecorder;
        this.clock = clock;
    }

    @Transactional
    public Certificate issue(UUID registrationId, UUID actorId, String actorEmail) {
        Registration registration = registrationPersistencePort.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Event event = eventPersistencePort.findById(registration.getEventId())
                .orElseThrow(() -> new EventNotFoundException(registration.getEventId()));
        Participant participant = participantPersistencePort.findById(registration.getParticipantId())
                .orElseThrow(() -> new ParticipantNotFoundException(registration.getParticipantId()));

        if (attendancePersistencePort.findByRegistrationId(registrationId).isEmpty()) {
            throw new CertificateExceptions.CertificateNotEligibleException(
                    "La inscripción no tiene asistencia registrada.");
        }
        if (event.getStatus() != EstadoEvento.FINALIZADO) {
            throw new CertificateExceptions.CertificateNotEligibleException(
                    "El evento debe estar FINALIZADO para emitir un certificado.");
        }
        if (certificatePersistencePort.findByRegistrationId(registrationId).isPresent()) {
            return certificatePersistencePort.findByRegistrationId(registrationId).orElseThrow();
        }

        String verificationCode = randomCode();
        Certificate pending = Certificate.issue(
                registrationId,
                event.getId(),
                participant.getId(),
                actorId,
                participant.getFirstName() + " " + participant.getLastName(),
                event.getName(),
                event.getStartAt(),
                event.getEndAt(),
                verificationCode);
        byte[] pdfBytes = pdfRenderer.render(pending, event, participant);
        String contentHash = Certificate.sha256(pending.canonicalPayload());
        String pdfHash = Certificate.sha256(java.util.Base64.getEncoder().encodeToString(pdfBytes));
        Certificate issued = pending.withHashes(contentHash, pdfHash);
        Certificate saved = certificatePersistencePort.save(issued);
        activityRecorder.record(actorId, actorEmail, "CERTIFICATE_ISSUED", LocalDateTime.now(clock));
        return saved;
    }

    public Certificate findById(UUID certificateId) {
        return certificatePersistencePort.findById(certificateId)
                .orElseThrow(() -> new CertificateExceptions.CertificateNotFoundException(certificateId));
    }

    public byte[] download(UUID certificateId) {
        Certificate certificate = findById(certificateId);
        if (certificate.status() == CertificateStatus.REVOCADO) {
            throw new CertificateExceptions.CertificateAlreadyRevokedException(certificate.verificationCode());
        }
        Registration registration = registrationPersistencePort.findById(certificate.registrationId())
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));
        Event event = eventPersistencePort.findById(registration.getEventId())
                .orElseThrow(() -> new EventNotFoundException(registration.getEventId()));
        Participant participant = participantPersistencePort.findById(registration.getParticipantId())
                .orElseThrow(() -> new ParticipantNotFoundException(registration.getParticipantId()));
        return pdfRenderer.render(certificate, event, participant);
    }

    @Transactional
    public Certificate revoke(UUID certificateId, String reason, UUID actorId, String actorEmail) {
        Certificate certificate = findById(certificateId);
        if (certificate.status() == CertificateStatus.REVOCADO) {
            throw new CertificateExceptions.CertificateAlreadyRevokedException(certificate.verificationCode());
        }
        if (reason == null || reason.isBlank()) {
            throw new CertificateExceptions.CertificateRevocationNotAllowedException(
                    "El motivo de revocación es obligatorio.");
        }
        Certificate revoked = certificate.revoke(reason, LocalDateTime.now(clock));
        Certificate saved = certificatePersistencePort.save(revoked);
        activityRecorder.record(actorId, actorEmail, "CERTIFICATE_REVOKED", LocalDateTime.now(clock));
        return saved;
    }

    private String randomCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}
