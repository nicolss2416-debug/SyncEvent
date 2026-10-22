package com.syncevent.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio: POJO puro, sin dependencias de Spring ni de JPA.
 * Representa un evento en el sistema SyncEvent.
 */
public class Event {

    private String id;
    private String name;
    private String description;
    private LocalDateTime dateTime;
    private String location;
    private Integer maxCapacity;
    private List<String> attendeeIds;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor vacío requerido por frameworks de persistencia/serialización.
     */
    public Event() {
        this.attendeeIds = new ArrayList<>();
    }

    /**
     * Crea un evento nuevo en estado {@link EventStatus#DRAFT}.
     *
     * @param name        nombre del evento (obligatorio)
     * @param description descripción del evento (opcional)
     * @param dateTime    fecha y hora del evento (obligatoria)
     * @param location    ubicación del evento (opcional)
     * @param maxCapacity aforo máximo; debe ser mayor que cero
     * @throws IllegalArgumentException si algún dato obligatorio no es válido
     */
    public Event(String name, String description, LocalDateTime dateTime, String location, Integer maxCapacity) {
        this();
        this.id = UUID.randomUUID().toString();
        this.name = requireText(name, "name");
        this.description = description;
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime no puede ser null");
        this.location = location;
        this.maxCapacity = requireCapacity(maxCapacity);
        this.status = EventStatus.DRAFT;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = Objects.requireNonNull(dateTime, "dateTime no puede ser null");
        touch();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        touch();
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = requireCapacity(maxCapacity);
        touch();
    }

    /**
     * Devuelve una copia inmutable de los identificadores de asistentes.
     * Evita que código externo modifique el estado interno del evento.
     */
    public List<String> getAttendeeIds() {
        return List.copyOf(attendeeIds);
    }

    /**
     * Reemplaza la lista de asistentes almacenando una copia defensiva.
     */
    public void setAttendeeIds(List<String> attendeeIds) {
        this.attendeeIds = attendeeIds == null
                ? new ArrayList<>()
                : new ArrayList<>(attendeeIds);
        touch();
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return value.trim();
    }

    private static Integer requireCapacity(Integer maxCapacity) {
        if (maxCapacity == null || maxCapacity <= 0) {
            throw new IllegalArgumentException("maxCapacity debe ser mayor que cero");
        }
        return maxCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event event)) {
            return false;
        }
        return id != null && Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dateTime=" + dateTime +
                ", location='" + location + '\'' +
                ", status=" + status +
                '}';
    }
}
