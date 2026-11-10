package com.syncevent.certificates.api;

import java.time.LocalDateTime;

import com.syncevent.certificates.application.CertificateVerificationService.CertificateVerificationResult;
import com.syncevent.certificates.domain.CertificateVerificationStatus;

public record CertificateVerificationResponse(
        CertificateVerificationStatus status,
        String verificationCode,
        String participantName,
        String participantDocumentType,
        String participantDocumentNumber,
        String eventName,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        String message,
        LocalDateTime issuedAt,
        boolean active) {

    public static CertificateVerificationResponse from(CertificateVerificationResult result) {
        return new CertificateVerificationResponse(
                result.status(),
                result.verificationCode(),
                result.participantName(),
                result.participantDocumentType(),
                result.participantDocumentNumber(),
                result.eventName(),
                result.eventStartAt(),
                result.eventEndAt(),
                result.message(),
                result.issuedAt(),
                result.active());
    }
}
