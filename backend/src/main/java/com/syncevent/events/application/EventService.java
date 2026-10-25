package com.syncevent.events.application;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
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
}