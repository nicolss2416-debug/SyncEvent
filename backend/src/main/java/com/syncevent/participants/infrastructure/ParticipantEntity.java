package com.syncevent.participants.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.participants.domain.Participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "participants", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_participants_document",
                columnNames = { "document_type", "document_number" })
})
public class ParticipantEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "created_by", nullable = false, length = 150, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ParticipantEntity() {
    }

    public UUID getId() {
        return id;
    }

    static ParticipantEntity fromDomain(Participant participant) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.id = participant.getId();
        entity.documentType = participant.getDocumentType();
        entity.documentNumber = participant.getDocumentNumber();
        entity.firstName = participant.getFirstName();
        entity.lastName = participant.getLastName();
        entity.email = participant.getEmail();
        entity.phone = participant.getPhone();
        entity.createdBy = participant.getCreatedBy();
        entity.createdAt = participant.getCreatedAt();
        entity.updatedBy = participant.getUpdatedBy();
        entity.updatedAt = participant.getUpdatedAt();
        return entity;
    }

    public static ParticipantEntity reference(UUID id) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.id = id;
        return entity;
    }

    public Participant toDomain() {
        return Participant.reconstitute(
                id,
                documentType,
                documentNumber,
                firstName,
                lastName,
                email,
                phone,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt);
    }
}
