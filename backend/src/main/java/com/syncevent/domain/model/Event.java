package com.syncevent.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class Event {

    private final UUID id;
    private final String name;
    private final String description;
    private final TipoEvento type;
    private final Modalidad modality;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final String location;
    private final String meetingUrl;
    private final int capacity;
    private final BigDecimal durationHours;
    private final EstadoEvento status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Event(
            UUID id,
            String name,
            String description,
            TipoEvento type,
            Modalidad modality,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String location,
            String meetingUrl,
            int capacity,
            BigDecimal durationHours,
            EstadoEvento status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id no puede ser null");
        this.name = requireText(name, "name");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type es obligatorio");
        this.modality = Objects.requireNonNull(modality, "modality es obligatoria");
        this.startAt = Objects.requireNonNull(startAt, "startAt es obligatorio");
        this.endAt = Objects.requireNonNull(endAt, "endAt es obligatorio");
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt debe ser posterior a startAt");
        }
        this.location = blankToNull(location);
        this.meetingUrl = blankToNull(meetingUrl);
        if (modality != Modalidad.VIRTUAL && this.location == null) {
            throw new IllegalArgumentException("location es obligatorio para esta modalidad");
        }
        if (modality != Modalidad.PRESENCIAL && this.meetingUrl == null) {
            throw new IllegalArgumentException("meetingUrl es obligatorio para esta modalidad");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity debe ser mayor que cero");
        }
        this.capacity = capacity;
        this.durationHours = Objects.requireNonNull(durationHours, "durationHours es obligatorio");
        if (durationHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("durationHours debe ser mayor que cero");
        }
        this.status = Objects.requireNonNull(status, "status es obligatorio");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt es obligatorio");
    }

    public static Event create(
            String name,
            String description,
            TipoEvento type,
            Modalidad modality,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String location,
            String meetingUrl,
            int capacity,
            BigDecimal durationHours) {
        LocalDateTime now = LocalDateTime.now();
        return new Event(
                UUID.randomUUID(),
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
                EstadoEvento.BORRADOR,
                now,
                now);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TipoEvento getType() {
        return type;
    }

    public Modalidad getModality() {
        return modality;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public String getLocation() {
        return location;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public EstadoEvento getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Event transitionTo(EstadoEvento targetStatus) {
        EstadoEvento expectedStatus = switch (status) {
            case BORRADOR -> EstadoEvento.PUBLICADO;
            case PUBLICADO -> EstadoEvento.EN_CURSO;
            case EN_CURSO -> EstadoEvento.FINALIZADO;
            case FINALIZADO, CANCELADO, APLAZADO -> null;
        };

        if (expectedStatus == null || targetStatus != expectedStatus) {
            String expected = expectedStatus == null
                    ? "ninguno; el evento está en un estado terminal o gestionado por otra operación"
                    : expectedStatus.name();
            throw new InvalidEventStateTransitionException(
                    "Transición no permitida desde %s. Estado permitido: %s."
                            .formatted(status, expected));
        }

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
                targetStatus,
                createdAt,
                LocalDateTime.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
