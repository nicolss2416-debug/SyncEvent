package com.syncevent.events.api;

import com.syncevent.domain.model.EstadoEvento;

import jakarta.validation.constraints.NotNull;

public record ChangeEventStatusRequest(@NotNull EstadoEvento status) {
}
