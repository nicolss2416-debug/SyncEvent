package com.syncevent.certificates.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificateStatus;

public record CertificateResponse(
        UUID id,
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        String verificationCode,
        String participantName,
        String eventName,
        LocalDateTime issuedAt,
        CertificateStatus status,
        String revocationReason) {

    public static CertificateResponse from(Certificate certificate) {
        return new CertificateResponse(
                certificate.id(),
                certificate.registrationId(),
                certificate.eventId(),
                certificate.participantId(),
                certificate.verificationCode(),
                certificate.participantName(),
                certificate.eventName(),
                certificate.issuedAt(),
                certificate.status(),
                certificate.revocationReason());
    }
}
