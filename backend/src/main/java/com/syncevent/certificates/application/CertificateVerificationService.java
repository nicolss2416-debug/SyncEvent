package com.syncevent.certificates.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificatePersistencePort;
import com.syncevent.certificates.domain.CertificateStatus;
import com.syncevent.certificates.domain.CertificateVerificationStatus;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.domain.model.Event;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;

@Service
public class CertificateVerificationService {

    private final CertificatePersistencePort certificatePersistencePort;
    private final RegistrationPersistencePort registrationPersistencePort;
    private final EventPersistencePort eventPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;

    public CertificateVerificationService(
            CertificatePersistencePort certificatePersistencePort,
            RegistrationPersistencePort registrationPersistencePort,
            EventPersistencePort eventPersistencePort,
            ParticipantPersistencePort participantPersistencePort) {
        this.certificatePersistencePort = certificatePersistencePort;
        this.registrationPersistencePort = registrationPersistencePort;
        this.eventPersistencePort = eventPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
    }

    public CertificateVerificationResult verify(String verificationCode) {
        Certificate certificate = certificatePersistencePort.findByVerificationCode(verificationCode)
                .orElse(null);
        if (certificate == null) {
            return CertificateVerificationResult.notFound(verificationCode);
        }
        if (certificate.status() == CertificateStatus.REVOCADO) {
            return CertificateVerificationResult.revoked(certificate);
        }
        Registration registration = registrationPersistencePort.findById(certificate.registrationId())
                .orElse(null);
        if (registration == null) {
            return CertificateVerificationResult.counterfeit(certificate, "La inscripción asociada ya no es válida.");
        }

        Event event = eventPersistencePort.findById(registration.getEventId()).orElse(null);
        Participant participant = participantPersistencePort.findById(registration.getParticipantId()).orElse(null);

        if (event == null || participant == null) {
            return CertificateVerificationResult.counterfeit(certificate, "La inscripción asociada ya no es válida.");
        }

        String expectedHash = Certificate.sha256(certificate.canonicalPayload());
        if (!expectedHash.equals(certificate.contentHash())) {
            return CertificateVerificationResult.counterfeit(certificate, "El certificado no coincide con su hash firmado.");
        }

        return CertificateVerificationResult.valid(certificate, event, participant);
    }

    @Transactional
    public Certificate revokeIfTampered(String verificationCode, String reason) {
        Certificate certificate = certificatePersistencePort.findByVerificationCode(verificationCode)
                .orElseThrow(() -> new CertificateExceptions.CertificateNotFoundException(verificationCode));
        String expectedHash = Certificate.sha256(certificate.canonicalPayload());
        if (!expectedHash.equals(certificate.contentHash())) {
            return certificatePersistencePort.save(certificate.revoke(reason, LocalDateTime.now()));
        }
        return certificate;
    }

    public record CertificateVerificationResult(
            CertificateVerificationStatus status,
            String verificationCode,
            String participantName,
            String eventName,
            String message,
            LocalDateTime issuedAt,
            boolean active) {

        static CertificateVerificationResult valid(Certificate certificate, Event event, Participant participant) {
            return new CertificateVerificationResult(
                    CertificateVerificationStatus.VALID,
                    certificate.verificationCode(),
                    participant.getFirstName() + " " + participant.getLastName(),
                    event.getName(),
                    "El certificado es válido.",
                    certificate.issuedAt(),
                    true);
        }

        static CertificateVerificationResult revoked(Certificate certificate) {
            return new CertificateVerificationResult(
                    CertificateVerificationStatus.REVOKED,
                    certificate.verificationCode(),
                    certificate.participantName(),
                    certificate.eventName(),
                    "El certificado está revocado: " + certificate.revocationReason(),
                    certificate.issuedAt(),
                    false);
        }

        static CertificateVerificationResult counterfeit(Certificate certificate, String reason) {
            return new CertificateVerificationResult(
                    CertificateVerificationStatus.COUNTERFEIT,
                    certificate.verificationCode(),
                    certificate.participantName(),
                    certificate.eventName(),
                    reason,
                    certificate.issuedAt(),
                    false);
        }

        static CertificateVerificationResult notFound(String verificationCode) {
            return new CertificateVerificationResult(
                    CertificateVerificationStatus.NOT_FOUND,
                    verificationCode,
                    null,
                    null,
                    "No existe un certificado con ese código.",
                    null,
                    false);
        }
    }
}
