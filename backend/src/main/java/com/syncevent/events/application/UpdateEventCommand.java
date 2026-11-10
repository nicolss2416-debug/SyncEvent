package com.syncevent.events.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.syncevent.events.domain.Modalidad;
import com.syncevent.events.domain.TipoEvento;

public record UpdateEventCommand(
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
