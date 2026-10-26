package com.syncevent.certificates.api;

import java.time.LocalDateTime;

import com.syncevent.certificates.application.CertificateVerificationService.CertificateVerificationResult;
import com.syncevent.certificates.domain.CertificateVerificationStatus;

public record CertificateVerificationResponse(
        CertificateVerificationStatus status,
        String verificationCode,
        String participantName,
        String eventName,
        String message,
        LocalDateTime issuedAt,
        boolean active) {

    public static CertificateVerificationResponse from(CertificateVerificationResult result) {
        return new CertificateVerificationResponse(
                result.status(),
                result.verificationCode(),
                result.participantName(),
                result.eventName(),
                result.message(),
                result.issuedAt(),
                result.active());
    }
}
