package com.syncevent.registrations.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.users.infrastructure.UserAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "registration_cancellation_history")
public class RegistrationCancellationHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "registration_id", nullable = false, unique = true, updatable = false)
    private UUID registrationId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, insertable = false, updatable = false)
    private RegistrationEntity registration;

    @Column(name = "canceled_by", nullable = false, updatable = false)
    private UUID canceledBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canceled_by", nullable = false, insertable = false, updatable = false)
    private UserAccountEntity canceledByUser;

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
