package com.syncevent.events.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_location_schedule", columnList = "location,start_at,end_at"),
        @Index(name = "idx_events_status", columnList = "status")
})
public class EventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEvento type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modalidad modality;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(length = 250)
    private String location;

    @Column(name = "meeting_url", length = 2048)
    private String meetingUrl;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "duration_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal durationHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EventEntity() {
    }

    public static EventEntity fromDomain(Event event) {
        EventEntity entity = new EventEntity();
        entity.id = event.getId();
        entity.name = event.getName();
        entity.description = event.getDescription();
        entity.type = event.getType();
        entity.modality = event.getModality();
        entity.startAt = event.getStartAt();
        entity.endAt = event.getEndAt();
        entity.location = event.getLocation();
        entity.meetingUrl = event.getMeetingUrl();
        entity.capacity = event.getCapacity();
        entity.durationHours = event.getDurationHours();
        entity.status = event.getStatus();
        entity.createdAt = event.getCreatedAt();
        entity.updatedAt = event.getUpdatedAt();
        return entity;
    }

    public Event toDomain() {
        return new Event(
                id,
                name,
                description,
                type,
                modality,
                startAt,
                endAt,
                location,
                meetingUrl,
                capacity,
                durationHours,
                status,
                createdAt,
                updatedAt);
    }
}