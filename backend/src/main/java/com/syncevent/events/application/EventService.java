package com.syncevent.events.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.EventPostponementNotAllowedException;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.events.domain.port.EventSearchCriteria;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.EventSort;
import com.syncevent.events.domain.port.SortDirection;

@Service
public class EventService {

    private final EventPersistencePort eventPersistencePort;

    public EventService(EventPersistencePort eventPersistencePort) {
        this.eventPersistencePort = eventPersistencePort;
    }

    @Transactional
    public Event create(CreateEventCommand command) {
        Event event = Event.create(
                command.name(),
                command.description(),
                command.type(),
                command.modality(),
                command.startAt(),
                command.endAt(),
                command.location(),
                command.meetingUrl(),
                command.capacity(),
                command.durationHours());

        if (event.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        event.getStartAt(), event.getEndAt(), event.getLocation())) {
            throw new EventScheduleConflictException();
        }

        return eventPersistencePort.save(event);
    }

    @Transactional(readOnly = true)
    public EventSearchPage search(
            TipoEvento type,
            Modalidad modality,
            EstadoEvento status,
            LocalDate from,
            LocalDate to,
            Set<EstadoEvento> visibleStatuses,
            int page,
            int size,
            EventSort sort,
            SortDirection direction) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        return eventPersistencePort.search(new EventSearchCriteria(
                type, modality, status, from, to, visibleStatuses, page, size, sort, direction));
    }

    @Transactional
    public Event changeStatus(UUID eventId, EstadoEvento targetStatus) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event transitioned = event.transitionTo(targetStatus);
        return eventPersistencePort.save(transitioned);
    }

    @Transactional
    public Event cancel(UUID eventId, String reason) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event canceled = event.cancel(reason);
        return eventPersistencePort.saveCancellation(canceled, event.getStatus(), reason);
    }

    @Transactional
    public Event postpone(UUID eventId, String reason, LocalDateTime newStartAt, LocalDateTime newEndAt) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event postponed = event.postpone(newStartAt, newEndAt, reason);

        if (postponed.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        postponed.getStartAt(), postponed.getEndAt(), postponed.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        return eventPersistencePort.savePostponementRequest(event, postponed, reason);
    }

    @Transactional
    public Event confirmPostponement(UUID eventId) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event confirmed = event.confirmPostponement();
        String reason = eventPersistencePort.findPendingPostponementReason(eventId)
                .orElseThrow(() -> new EventPostponementNotAllowedException(
                        "No hay una solicitud de aplazamiento pendiente."));

        if (confirmed.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        confirmed.getStartAt(), confirmed.getEndAt(), confirmed.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        return eventPersistencePort.savePostponementConfirmation(
                event, confirmed, reason);
    }

    @Transactional
    public Event update(UUID eventId, UpdateEventCommand command) {
        Event existing = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event updated = existing.updateDetails(
                command.name(),
                command.description(),
                command.type(),
                command.modality(),
                command.startAt(),
                command.endAt(),
                command.location(),
                command.meetingUrl(),
                command.capacity(),
                command.durationHours());

        if (updated.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        updated.getStartAt(), updated.getEndAt(), updated.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        return eventPersistencePort.save(updated);
    }
}