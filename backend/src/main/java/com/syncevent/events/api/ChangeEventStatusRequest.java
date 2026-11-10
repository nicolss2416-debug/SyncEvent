package com.syncevent.events.api;

import com.syncevent.events.domain.EstadoEvento;

import jakarta.validation.constraints.NotNull;

public record ChangeEventStatusRequest(@NotNull EstadoEvento status) {
}
