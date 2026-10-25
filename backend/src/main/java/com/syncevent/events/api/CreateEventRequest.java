package com.syncevent.events.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.application.CreateEventCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5000) String description,
        @NotNull TipoEvento type,
        @NotNull Modalidad modality,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Size(max = 250) String location,
        @Size(max = 2048) String meetingUrl,
        @Positive int capacity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal durationHours) {

    public CreateEventCommand toCommand() {
        return new CreateEventCommand(
                name,
                description,
                type,
                modality,
                startAt,
                endAt,
                location,
                meetingUrl,
                capacity,
                durationHours);
    }
}