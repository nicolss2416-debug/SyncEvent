package com.syncevent.certificates.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

public record Certificate(
        UUID id,
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        UUID issuedBy,
        String verificationCode,
        String participantName,
        String eventName,
        LocalDateTime issuedAt,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        String contentHash,
        String pdfHash,
        CertificateStatus status,
        LocalDateTime revokedAt,
        String revocationReason) {

    public static Certificate issue(
            UUID registrationId,
            UUID eventId,
            UUID participantId,
            UUID issuedBy,
            String participantName,
            String eventName,
            LocalDateTime eventStartAt,
            LocalDateTime eventEndAt,
            String verificationCode) {
        LocalDateTime issuedAt = LocalDateTime.now();
        return new Certificate(
                UUID.randomUUID(),
                registrationId,
                eventId,
                participantId,
                issuedBy,
                verificationCode,
                participantName,
                eventName,
                issuedAt,
                eventStartAt,
                eventEndAt,
                null,
                null,
                CertificateStatus.VIGENTE,
                null,
                null);
    }

    public Certificate withHashes(String contentHash, String pdfHash) {
        return new Certificate(
                id,
                registrationId,
                eventId,
                participantId,
                issuedBy,
                verificationCode,
                participantName,
                eventName,
                issuedAt,
                eventStartAt,
                eventEndAt,
                contentHash,
                pdfHash,
                status,
                revokedAt,
                revocationReason);
    }

    public Certificate revoke(String reason, LocalDateTime revokedAt) {
        if (status == CertificateStatus.REVOCADO) {
            throw new IllegalStateException("El certificado ya está revocado.");
        }
        return new Certificate(
                id,
                registrationId,
                eventId,
                participantId,
                issuedBy,
                verificationCode,
                participantName,
                eventName,
                issuedAt,
                eventStartAt,
                eventEndAt,
                contentHash,
                pdfHash,
                CertificateStatus.REVOCADO,
                revokedAt,
                reason);
    }

    public boolean isRevoked() {
        return status == CertificateStatus.REVOCADO;
    }

    public String signedHash() {
        return contentHash == null ? sha256(canonicalPayload()) : contentHash;
    }

    public String canonicalPayload() {
        return String.join("|",
                participantName,
                eventName,
                eventStartAt.toString(),
                eventEndAt.toString(),
                verificationCode,
                registrationId.toString(),
                participantId.toString(),
                eventId.toString());
    }

    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            for (byte current : hash) {
                output.append(String.format("%02x", current));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo calcular el hash del certificado.", exception);
        }
    }
}
