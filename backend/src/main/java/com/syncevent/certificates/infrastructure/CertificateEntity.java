package com.syncevent.certificates.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificateStatus;
import com.syncevent.registrations.infrastructure.RegistrationEntity;
import com.syncevent.users.infrastructure.UserAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "certificate_documents")
public class CertificateEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "registration_id", nullable = false, unique = true, updatable = false)
    private UUID registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, insertable = false, updatable = false)
    private RegistrationEntity registration;

    @Transient
    private UUID transientEventId;

    @Transient
    private UUID transientParticipantId;

    @Transient
    private LocalDateTime transientEventStartAt;

    @Transient
    private LocalDateTime transientEventEndAt;

    @Column(name = "issued_by", nullable = false, updatable = false)
    private UUID issuedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false, insertable = false, updatable = false)
    private UserAccountEntity issuedByUser;

    @Column(name = "verification_code", nullable = false, unique = true, length = 64)
    private String verificationCode;

    @Column(name = "participant_name", nullable = false, length = 255)
    private String participantName;

    @Column(name = "participant_document_type", nullable = false, length = 40)
    private String participantDocumentType;

    @Column(name = "participant_document_number", nullable = false, length = 100)
    private String participantDocumentNumber;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

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
        entity.transientEventId = certificate.eventId();
        entity.transientParticipantId = certificate.participantId();
        entity.issuedBy = certificate.issuedBy();
        entity.verificationCode = certificate.verificationCode();
        entity.participantName = certificate.participantName();
        entity.participantDocumentType = certificate.participantDocumentType();
        entity.participantDocumentNumber = certificate.participantDocumentNumber();
        entity.eventName = certificate.eventName();
        entity.issuedAt = certificate.issuedAt();
        entity.transientEventStartAt = certificate.eventStartAt();
        entity.transientEventEndAt = certificate.eventEndAt();
        entity.contentHash = certificate.contentHash();
        entity.pdfHash = certificate.pdfHash();
        entity.status = certificate.status();
        entity.revokedAt = certificate.revokedAt();
        entity.revocationReason = certificate.revocationReason();
        return entity;
    }

    public Certificate toDomain() {
        UUID resolvedEventId = registration != null && registration.getEvent() != null
                ? registration.getEvent().getId()
                : transientEventId;
        UUID resolvedParticipantId = registration != null && registration.getParticipant() != null
                ? registration.getParticipant().getId()
                : transientParticipantId;
        LocalDateTime resolvedStartAt = registration != null && registration.getEvent() != null
                ? registration.getEvent().getStartAt()
                : transientEventStartAt;
        LocalDateTime resolvedEndAt = registration != null && registration.getEvent() != null
                ? registration.getEvent().getEndAt()
                : transientEventEndAt;
        return new Certificate(
                id,
                registrationId,
                resolvedEventId,
                resolvedParticipantId,
                issuedBy,
                verificationCode,
                participantName,
                participantDocumentType,
                participantDocumentNumber,
                eventName,
                issuedAt,
                resolvedStartAt,
                resolvedEndAt,
                contentHash,
                pdfHash,
                status,
                revokedAt,
                revocationReason);
    }
}
