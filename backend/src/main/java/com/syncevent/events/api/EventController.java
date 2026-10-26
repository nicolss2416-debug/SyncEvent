package com.syncevent.events.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.application.EventService;
import com.syncevent.events.application.MonitorAssignmentService;
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
    private final MonitorAssignmentService monitorAssignmentService;

    public EventController(EventService eventService, MonitorAssignmentService monitorAssignmentService) {
        this.eventService = eventService;
        this.monitorAssignmentService = monitorAssignmentService;
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

    @PatchMapping("/{eventId}/status")
    public EventResponse changeStatus(
            @PathVariable UUID eventId,
            @Valid @RequestBody ChangeEventStatusRequest request) {
        return EventResponse.from(eventService.changeStatus(eventId, request.status()));
    }

    @PostMapping("/{eventId}/cancellation")
    public EventResponse cancel(
            @PathVariable UUID eventId,
            @Valid @RequestBody CancelEventRequest request) {
        return EventResponse.from(eventService.cancel(eventId, request.reason()));
    }

    @PostMapping("/{eventId}/postponements")
    public EventResponse postpone(
            @PathVariable UUID eventId,
            @Valid @RequestBody PostponeEventRequest request) {
        return EventResponse.from(eventService.postpone(
                eventId, request.reason(), request.newStartAt(), request.newEndAt()));
    }

    @PostMapping("/{eventId}/postponements/confirmation")
    public EventResponse confirmPostponement(@PathVariable UUID eventId) {
        return EventResponse.from(eventService.confirmPostponement(eventId));
    }

    @PutMapping("/{eventId}")
    public EventResponse update(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        return EventResponse.from(eventService.update(eventId, request.toCommand()));
    }

    @GetMapping("/{eventId}/monitors")
    public java.util.List<MonitorAvailabilityResponse> findMonitorAvailability(@PathVariable UUID eventId) {
        return monitorAssignmentService.findAvailability(eventId).stream()
                .map(MonitorAvailabilityResponse::from)
                .toList();
    }

    @PostMapping("/{eventId}/monitors")
    public ResponseEntity<MonitorAssignmentResponse> assignMonitor(
            @PathVariable UUID eventId,
            @Valid @RequestBody AssignMonitorRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(MonitorAssignmentResponse.from(monitorAssignmentService.assign(
                        eventId,
                        request.monitorId(),
                        UUID.fromString(currentToken.getClaimAsString("uid")),
                        currentToken.getSubject())));
    }

    @DeleteMapping("/{eventId}/monitors/{monitorId}")
    public ResponseEntity<Void> removeMonitor(
            @PathVariable UUID eventId,
            @PathVariable UUID monitorId,
            @AuthenticationPrincipal Jwt currentToken) {
        monitorAssignmentService.remove(
                eventId,
                monitorId,
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject());
        return ResponseEntity.noContent().build();
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