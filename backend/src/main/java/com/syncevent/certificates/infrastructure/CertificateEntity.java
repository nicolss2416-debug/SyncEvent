package com.syncevent.certificates.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificateStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "certificate_documents")
public class CertificateEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "registration_id", nullable = false, unique = true, updatable = false)
    private UUID registrationId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "issued_by", nullable = false, updatable = false)
    private UUID issuedBy;

    @Column(name = "verification_code", nullable = false, unique = true, length = 64)
    private String verificationCode;

    @Column(name = "participant_name", nullable = false, length = 255)
    private String participantName;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "event_start_at", nullable = false, updatable = false)
    private LocalDateTime eventStartAt;

    @Column(name = "event_end_at", nullable = false, updatable = false)
    private LocalDateTime eventEndAt;

    @Column(name = "content_hash", length = 128, nullable = false)
    private String contentHash;

    @Column(name = "pdf_hash", length = 128, nullable = false)
    private String pdfHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", length = 255)
    private String revocationReason;

    protected CertificateEntity() {
    }

    public static CertificateEntity fromDomain(Certificate certificate) {
        CertificateEntity entity = new CertificateEntity();
        entity.id = certificate.id();
        entity.registrationId = certificate.registrationId();
        entity.eventId = certificate.eventId();
        entity.participantId = certificate.participantId();
        entity.issuedBy = certificate.issuedBy();
        entity.verificationCode = certificate.verificationCode();
        entity.participantName = certificate.participantName();
        entity.eventName = certificate.eventName();
        entity.issuedAt = certificate.issuedAt();
        entity.eventStartAt = certificate.eventStartAt();
        entity.eventEndAt = certificate.eventEndAt();
        entity.contentHash = certificate.contentHash();
        entity.pdfHash = certificate.pdfHash();
        entity.status = certificate.status();
        entity.revokedAt = certificate.revokedAt();
        entity.revocationReason = certificate.revocationReason();
        return entity;
    }

    public Certificate toDomain() {
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
}
