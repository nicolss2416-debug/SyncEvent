package com.syncevent.events.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
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

@Controller
public class EventGraphQlController {

    private final EventService eventService;

    public EventGraphQlController(EventService eventService) {
        this.eventService = eventService;
    }

    @QueryMapping("events")
    public List<EventDto> events(
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        EstadoEvento estado = (status != null && !status.isBlank() && !status.equalsIgnoreCase("TODOS"))
                ? EstadoEvento.valueOf(status.toUpperCase())
                : null;
        int p = page != null ? page : 0;
        int s = size != null ? size : 100;
        EventSearchPage result = eventService.search(
                null, null, estado, null, null, null, p, s, EventSort.DATE, SortDirection.DESC);
        return result.content().stream().map(EventDto::from).toList();
    }

    @QueryMapping("event")
    public EventDto event(@Argument UUID id) {
        return eventService.findById(id).map(EventDto::from).orElse(null);
    }

    @QueryMapping("dashboardSummary")
    public DashboardSummary dashboardSummary() {
        List<Event> allEvents = eventService.findAll();
        int totalEvents = allEvents.size();
        int activeEvents = (int) allEvents.stream()
                .filter(e -> e.getStatus() == EstadoEvento.PUBLICADO || e.getStatus() == EstadoEvento.EN_CURSO)
                .count();
        return new DashboardSummary(totalEvents, activeEvents, 4, 3);
    }

    @MutationMapping("createEvent")
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
        if (raw == null || raw.isBlank()) return TipoEvento.TALLER;
        try {
            return TipoEvento.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TipoEvento.TALLER;
        }
    }

    private Modalidad parseModalidad(String raw) {
        if (raw == null || raw.isBlank()) return Modalidad.PRESENCIAL;
        try {
            return Modalidad.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Modalidad.PRESENCIAL;
        }
    }

    @MutationMapping("updateEventStatus")
    public EventDto updateEventStatus(@Argument UUID eventId, @Argument String status) {
        EstadoEvento estado = EstadoEvento.valueOf(status.toUpperCase());
        Event updated = eventService.changeStatus(eventId, estado);
        return EventDto.from(updated);
    }

    @MutationMapping("cancelEvent")
    public EventDto cancelEvent(@Argument UUID eventId, @Argument String reason) {
        Event cancelled = eventService.cancel(eventId, reason != null ? reason : "Cancelado desde GraphQL");
        return EventDto.from(cancelled);
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
