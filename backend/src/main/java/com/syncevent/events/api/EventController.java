package com.syncevent.events.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.application.EventService;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.EventSort;
import com.syncevent.events.domain.port.SortDirection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/events")
@Validated
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        Event event = eventService.create(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(event.getId())
                .toUri();
        return ResponseEntity.created(location).body(EventResponse.from(event));
    }

    @GetMapping
    public EventPageResponse search(
            @RequestParam(required = false) TipoEvento type,
            @RequestParam(required = false) Modalidad modality,
            @RequestParam(required = false) EstadoEvento status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "DATE") EventSort sort,
            @RequestParam(defaultValue = "ASC") SortDirection direction,
            Authentication authentication) {
        Set<EstadoEvento> visibleStatuses = visibleStatuses(authentication);
        EventSearchPage result = eventService.search(
                type, modality, status, from, to, visibleStatuses,
                page, size, sort, direction);
        return EventPageResponse.from(result);
    }

    private Set<EstadoEvento> visibleStatuses(Authentication authentication) {
        boolean isAdminOrOperator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || authority.getAuthority().equals("ROLE_OPERADOR"));
        if (isAdminOrOperator) {
            return null;
        }

        boolean isMonitor = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MONITOR"));
        if (isMonitor) {
            return Set.of(EstadoEvento.PUBLICADO, EstadoEvento.EN_CURSO);
        }
        return Set.of(EstadoEvento.PUBLICADO);
    }
}