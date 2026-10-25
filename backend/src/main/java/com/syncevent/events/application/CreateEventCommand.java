package com.syncevent.events.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;

public record CreateEventCommand(
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
}