package com.syncevent.events.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
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

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.application.CreateEventCommand;
import com.syncevent.events.application.EventService;
import com.syncevent.events.domain.port.EventSearchCriteria;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.EventSort;
import com.syncevent.events.domain.port.SortDirection;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.RegistrationPersistencePort;

@Controller
public class EventGraphQlController {

    private final EventService eventService;
    private final EventPersistencePort eventPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final RegistrationPersistencePort registrationPersistencePort;

    public EventGraphQlController(
            EventService eventService,
            EventPersistencePort eventPersistencePort,
            ParticipantPersistencePort participantPersistencePort,
            RegistrationPersistencePort registrationPersistencePort) {
        this.eventService = eventService;
        this.eventPersistencePort = eventPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.registrationPersistencePort = registrationPersistencePort;
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
        return result.content().stream().map(EventDto::from).toList();
    }

    @QueryMapping("event")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR', 'MONITOR', 'INVITADO')")
    public EventDto event(@Argument UUID id) {
        Set<EstadoEvento> visibleStatuses = visibleStatuses();
        return eventService.findById(id)
                .filter(event -> visibleStatuses == null || visibleStatuses.contains(event.getStatus()))
                .map(EventDto::from)
                .orElse(null);
    }

    @QueryMapping("dashboardSummary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public DashboardSummary dashboardSummary() {
        int totalEvents = Math.toIntExact(eventPersistencePort.countAll());
        int activeEvents = Math.toIntExact(eventPersistencePort.countByStatuses(
            EnumSet.of(EstadoEvento.PUBLICADO, EstadoEvento.EN_CURSO)));
        int totalParticipants = Math.toIntExact(participantPersistencePort.countAll());
        int totalRegistrations = Math.toIntExact(registrationPersistencePort.countAll());
        return new DashboardSummary(totalEvents, activeEvents, totalParticipants, totalRegistrations);
    }

    @MutationMapping("createEvent")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public EventDto createEvent(@Argument CreateEventInput input) {
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
        return EventDto.from(created);
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
        EstadoEvento estado = EstadoEvento.valueOf(status.toUpperCase());
        Event updated = eventService.changeStatus(eventId, estado);
        return EventDto.from(updated);
    }

    @MutationMapping("cancelEvent")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public EventDto cancelEvent(@Argument UUID eventId, @Argument String reason) {
        Event cancelled = eventService.cancel(eventId, reason != null ? reason : "Cancelado desde GraphQL");
        return EventDto.from(cancelled);
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
            String createdAt,
            String updatedAt) {

        public static EventDto from(Event event) {
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
                    event.getCreatedAt() != null ? event.getCreatedAt().toString() : null,
                    event.getUpdatedAt() != null ? event.getUpdatedAt().toString() : null);
        }
    }
}
