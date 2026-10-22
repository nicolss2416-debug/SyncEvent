package com.syncevent.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evento de dominio - POJO puro sin dependencias de Spring o JPA.
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

    // Constructor por defecto
    public Event() {
    }

    // Constructor con parámetros principales
    public Event(String name, String description, LocalDateTime dateTime, String location, Integer maxCapacity) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.dateTime = dateTime;
        this.location = location;
        this.maxCapacity = maxCapacity;
        this.status = EventStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters y setters
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
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getAttendeeIds() {
        return attendeeIds;
    }

    public void setAttendeeIds(List<String> attendeeIds) {
        this.attendeeIds = attendeeIds;
        this.updatedAt = LocalDateTime.now();
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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