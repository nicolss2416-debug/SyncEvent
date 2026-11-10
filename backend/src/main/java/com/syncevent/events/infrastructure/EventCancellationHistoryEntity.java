package com.syncevent.events.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.events.domain.EstadoEvento;
import com.syncevent.events.domain.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_cancellation_history")
public class EventCancellationHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, insertable = false, updatable = false)
    private EventEntity event;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, updatable = false, length = 20)
    private EstadoEvento previousStatus;

    @Column(nullable = false, updatable = false, length = 2000)
    private String reason;

    @Column(name = "canceled_at", nullable = false, updatable = false)
    private LocalDateTime canceledAt;

    protected EventCancellationHistoryEntity() {
    }

    static EventCancellationHistoryEntity create(
            Event canceledEvent, EstadoEvento previousStatus, String reason) {
        EventCancellationHistoryEntity history = new EventCancellationHistoryEntity();
        history.id = UUID.randomUUID();
        history.eventId = canceledEvent.getId();
        history.previousStatus = previousStatus;
        history.reason = reason.trim();
        history.canceledAt = canceledEvent.getUpdatedAt();
        return history;
    }
}
