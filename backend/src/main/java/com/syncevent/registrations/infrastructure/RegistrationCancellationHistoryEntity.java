package com.syncevent.registrations.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_cancellation_history")
public class RegistrationCancellationHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "registration_id", nullable = false, updatable = false)
    private UUID registrationId;

    @Column(name = "canceled_by", nullable = false, updatable = false)
    private UUID canceledBy;

    @Column(nullable = false, length = 2000, updatable = false)
    private String reason;

    @Column(name = "canceled_at", nullable = false, updatable = false)
    private LocalDateTime canceledAt;

    protected RegistrationCancellationHistoryEntity() {
    }

    static RegistrationCancellationHistoryEntity create(
            UUID registrationId, UUID canceledBy, String reason, LocalDateTime canceledAt) {
        RegistrationCancellationHistoryEntity entity = new RegistrationCancellationHistoryEntity();
        entity.id = UUID.randomUUID();
        entity.registrationId = registrationId;
        entity.canceledBy = canceledBy;
        entity.reason = reason;
        entity.canceledAt = canceledAt;
        return entity;
    }
}
