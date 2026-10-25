package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.domain.model.Event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_postponement_history")
public class EventPostponementHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false, length = 30)
    private String action;

    @Column(name = "previous_start_at", nullable = false, updatable = false)
    private LocalDateTime previousStartAt;

    @Column(name = "previous_end_at", nullable = false, updatable = false)
    private LocalDateTime previousEndAt;

    @Column(name = "new_start_at", nullable = false, updatable = false)
    private LocalDateTime newStartAt;

    @Column(name = "new_end_at", nullable = false, updatable = false)
    private LocalDateTime newEndAt;

    @Column(nullable = false, updatable = false, length = 2000)
    private String reason;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    protected EventPostponementHistoryEntity() {
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    static EventPostponementHistoryEntity requested(
            Event previousEvent, Event postponedEvent, String reason) {
        return create("SOLICITADO", previousEvent, postponedEvent, reason);
    }

    static EventPostponementHistoryEntity confirmed(
            Event postponedEvent, Event confirmedEvent, String reason) {
        return create("CONFIRMADO", postponedEvent, confirmedEvent, reason);
    }

    private static EventPostponementHistoryEntity create(
            String action, Event previousEvent, Event currentEvent, String reason) {
        EventPostponementHistoryEntity history = new EventPostponementHistoryEntity();
        history.eventId = currentEvent.getId();
        history.action = action;
        history.previousStartAt = previousEvent.getStartAt();
        history.previousEndAt = previousEvent.getEndAt();
        history.newStartAt = currentEvent.getStartAt();
        history.newEndAt = currentEvent.getEndAt();
        history.reason = reason.trim();
        history.recordedAt = LocalDateTime.now();
        return history;
    }
}
