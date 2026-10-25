package com.syncevent.events.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;

public record EventResponse(
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

    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getType(),
                event.getModality(),
                event.getStartAt(),
                event.getEndAt(),
                event.getLocation(),
                event.getMeetingUrl(),
                event.getCapacity(),
                event.getDurationHours(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}