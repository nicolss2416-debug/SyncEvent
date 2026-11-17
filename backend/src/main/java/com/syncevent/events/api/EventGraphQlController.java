package com.syncevent.events.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.syncevent.audit.api.GraphQlAuditRecorder;
import com.syncevent.events.domain.EstadoEvento;
import com.syncevent.events.domain.Event;
import com.syncevent.events.domain.Modalidad;
import com.syncevent.events.domain.TipoEvento;
import com.syncevent.events.application.CreateEventCommand;
import com.syncevent.events.application.EventService;
import com.syncevent.events.domain.EventSearchPage;
import com.syncevent.events.domain.EventSort;
import com.syncevent.events.domain.SortDirection;
import com.syncevent.participants.application.ParticipantService;
import com.syncevent.registrations.application.RegistrationService;

@Controller
public class EventGraphQlController {

    private final EventService eventService;
    private final ParticipantService participantService;
    private final RegistrationService registrationService;
    private final GraphQlAuditRecorder graphQlAuditRecorder;

    public EventGraphQlController(
            EventService eventService,
            ParticipantService participantService,
            RegistrationService registrationService,
            GraphQlAuditRecorder graphQlAuditRecorder) {
        this.eventService = eventService;
        this.participantService = participantService;
        this.registrationService = registrationService;
        this.graphQlAuditRecorder = graphQlAuditRecorder;
    }

    @QueryMapping("events")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'MONITOR', 'INVITADO')")
    public List<EventDto> events(
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        EstadoEvento estado = (status != null && !status.isBlank() && !status.equalsIgnoreCase("TODOS"))
                ? EstadoEvento.valueOf(status.toUpperCase())
                : null;
        int p = page != null ? page : 0;
        int s = size != null ? size : 100;
        Set<EstadoEvento> visibleStatuses = visibleStatuses();
        EventSearchPage result = eventService.search(
                null, null, estado, null, null, visibleStatuses, p, s, EventSort.DATE, SortDirection.DESC);
        Map<UUID, List<String>> monitorsByEvent = eventService.findAssignedMonitorEmails(
                result.content().stream().map(Event::getId).toList());
        return result.content().stream()
                .map(event -> EventDto.from(
                        event,
                        Math.toIntExact(eventService.countActiveRegistrations(event.getId())),
                        monitorsByEvent.getOrDefault(event.getId(), List.of())))
                .toList();
    }

    @QueryMapping("event")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'MONITOR', 'INVITADO')")
    public EventDto event(@Argument UUID id) {
        Set<EstadoEvento> visibleStatuses = visibleStatuses();
        return eventService.findById(id)
                .filter(event -> visibleStatuses == null || visibleStatuses.contains(event.getStatus()))
                .map(event -> EventDto.from(
                        event,
                        Math.toIntExact(eventService.countActiveRegistrations(event.getId())),
                        assignedMonitors(event.getId())))
                .orElse(null);
    }

    @QueryMapping("dashboardSummary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public DashboardSummary dashboardSummary() {
        int totalEvents = Math.toIntExact(eventService.countAll());
        int activeEvents = Math.toIntExact(eventService.countActiveEvents());
        int totalParticipants = Math.toIntExact(participantService.countAll());
        int totalRegistrations = Math.toIntExact(registrationService.countAll());
        return new DashboardSummary(totalEvents, activeEvents, totalParticipants, totalRegistrations);
    }

    @MutationMapping("createEvent")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public EventDto createEvent(@Argument CreateEventInput input) {
        try {
            CreateEventCommand cmd = new CreateEventCommand(
                    input.name(),
                    input.description(),
                    parseTipoEvento(input.type()),
                    parseModalidad(input.modality()),
                    LocalDateTime.parse(input.startAt()),
                    LocalDateTime.parse(input.endAt()),
                    input.location(),
                    input.meetingUrl(),
                    input.capacity(),
                    BigDecimal.valueOf(input.durationHours()));
            Event created = eventService.create(cmd);
            graphQlAuditRecorder.success(
                    "EVENTO_CREADO", "EVENTO", created.getId(), null,
                    "GraphQL mutation createEvent");
            return EventDto.from(created, 0, List.of());
        } catch (RuntimeException exception) {
            graphQlAuditRecorder.failure(
                    "EVENTO_CREADO", "EVENTO", null, null,
                    failureDetails("createEvent", exception));
            throw exception;
        }
    }

    private TipoEvento parseTipoEvento(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("El tipo de evento es obligatorio.");
        try {
            return TipoEvento.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El tipo de evento no es válido.", e);
        }
    }

    private Modalidad parseModalidad(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("La modalidad del evento es obligatoria.");
        try {
            return Modalidad.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La modalidad del evento no es válida.", e);
        }
    }

    @MutationMapping("updateEventStatus")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public EventDto updateEventStatus(@Argument UUID eventId, @Argument String status) {
        try {
            if (status == null || status.isBlank()) {
                throw new IllegalArgumentException("El estado es obligatorio.");
            }
            EstadoEvento estado = EstadoEvento.valueOf(status.trim().toUpperCase());
            Event updated = eventService.changeStatus(eventId, estado);
            graphQlAuditRecorder.success(
                    "ESTADO_EVENTO_CAMBIADO", "EVENTO", updated.getId(), null,
                    "GraphQL mutation updateEventStatus -> " + estado.name());
            return EventDto.from(
                    updated,
                    Math.toIntExact(eventService.countActiveRegistrations(updated.getId())),
                    assignedMonitors(updated.getId()));
        } catch (RuntimeException exception) {
            graphQlAuditRecorder.failure(
                    "ESTADO_EVENTO_CAMBIADO", "EVENTO", eventId, null,
                    failureDetails("updateEventStatus", exception));
            throw exception;
        }
    }

    @MutationMapping("cancelEvent")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public EventDto cancelEvent(@Argument UUID eventId, @Argument String reason) {
        try {
            Event cancelled = eventService.cancel(eventId, reason != null ? reason : "Cancelado desde GraphQL");
            graphQlAuditRecorder.success(
                    "EVENTO_CANCELADO", "EVENTO", cancelled.getId(), reason,
                    "GraphQL mutation cancelEvent");
            return EventDto.from(
                    cancelled,
                    Math.toIntExact(eventService.countActiveRegistrations(cancelled.getId())),
                    assignedMonitors(cancelled.getId()));
        } catch (RuntimeException exception) {
            graphQlAuditRecorder.failure(
                    "EVENTO_CANCELADO", "EVENTO", eventId, reason,
                    failureDetails("cancelEvent", exception));
            throw exception;
        }
    }

    private List<String> assignedMonitors(UUID eventId) {
        return eventService.findAssignedMonitorEmails(List.of(eventId)).getOrDefault(eventId, List.of());
    }

    private String failureDetails(String mutation, RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "GraphQL mutation " + mutation + " falló ("
                    + exception.getClass().getSimpleName() + ")";
        }
        return "GraphQL mutation " + mutation + " falló: " + message;
    }

    private Set<EstadoEvento> visibleStatuses() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        if (roles.contains("ROLE_ADMINISTRADOR") || roles.contains("ROLE_OPERADOR")) {
            return null;
        }

        EnumSet<EstadoEvento> visible = EnumSet.noneOf(EstadoEvento.class);
        if (roles.contains("ROLE_MONITOR")) {
            visible.add(EstadoEvento.PUBLICADO);
            visible.add(EstadoEvento.EN_CURSO);
        }
        if (roles.contains("ROLE_INVITADO")) {
            visible.add(EstadoEvento.PUBLICADO);
        }
        return visible;
    }

    public record CreateEventInput(
            String name,
            String description,
            String type,
            String modality,
            String startAt,
            String endAt,
            String location,
            String meetingUrl,
            int capacity,
            double durationHours) {}

    public record DashboardSummary(
            int totalEvents,
            int activeEvents,
            int totalParticipants,
            int totalRegistrations) {}

    public record EventDto(
            UUID id,
            String name,
            String description,
            String type,
            String modality,
            String startAt,
            String endAt,
            String location,
            String meetingUrl,
            int capacity,
            double durationHours,
            String status,
            int registered,
            List<String> assignedMonitors,
            String createdAt,
            String updatedAt) {

        public static EventDto from(Event event, int registered) {
            return from(event, registered, List.of());
        }

        public static EventDto from(Event event, int registered, List<String> assignedMonitors) {
            return new EventDto(
                    event.getId(),
                    event.getName(),
                    event.getDescription(),
                    event.getType() != null ? event.getType().name() : "",
                    event.getModality() != null ? event.getModality().name() : "",
                    event.getStartAt() != null ? event.getStartAt().toString() : "",
                    event.getEndAt() != null ? event.getEndAt().toString() : "",
                    event.getLocation(),
                    event.getMeetingUrl(),
                    event.getCapacity(),
                    event.getDurationHours() != null ? event.getDurationHours().doubleValue() : 0.0,
                    event.getStatus() != null ? event.getStatus().name() : "",
                    registered,
                    assignedMonitors != null ? assignedMonitors : List.of(),
                    event.getCreatedAt() != null ? event.getCreatedAt().toString() : null,
                    event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null);
        }
    }
}
