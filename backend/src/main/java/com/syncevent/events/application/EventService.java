package com.syncevent.events.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.attendance.application.AttendanceLiveUpdatePort;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.EventPostponementNotAllowedException;
import com.syncevent.domain.model.EventModificationNotAllowedException;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.events.domain.port.EventFinalizationPort;
import com.syncevent.events.domain.port.ActiveRegistrationCountPort;
import com.syncevent.events.domain.port.EventSearchCriteria;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.EventSort;
import com.syncevent.events.domain.port.SortDirection;
import com.syncevent.events.domain.port.MonitorSchedulePort;

@Service
public class EventService {

    private final EventPersistencePort eventPersistencePort;
    private final ActiveRegistrationCountPort activeRegistrationCountPort;
    private final EventFinalizationPort eventFinalizationPort;
    private final MonitorSchedulePort monitorSchedulePort;
    private final AttendanceLiveUpdatePort attendanceLiveUpdatePort;

    public EventService(
            EventPersistencePort eventPersistencePort,
            ActiveRegistrationCountPort activeRegistrationCountPort,
            EventFinalizationPort eventFinalizationPort,
            MonitorSchedulePort monitorSchedulePort,
            AttendanceLiveUpdatePort attendanceLiveUpdatePort) {
        this.eventPersistencePort = eventPersistencePort;
        this.activeRegistrationCountPort = activeRegistrationCountPort;
        this.eventFinalizationPort = eventFinalizationPort;
        this.monitorSchedulePort = monitorSchedulePort;
        this.attendanceLiveUpdatePort = attendanceLiveUpdatePort;
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
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event transitioned = event.transitionTo(targetStatus);
        Event saved = eventPersistencePort.save(transitioned);
        attendanceLiveUpdatePort.publishAfterCommit(eventId);
        if (event.getStatus() == EstadoEvento.EN_CURSO && targetStatus == EstadoEvento.FINALIZADO) {
            eventFinalizationPort.processFinalizedEvent(eventId);
        }
        return saved;
    }

    @Transactional
    public Event cancel(UUID eventId, String reason) {
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event canceled = event.cancel(reason);
        Event saved = eventPersistencePort.saveCancellation(canceled, event.getStatus(), reason);
        attendanceLiveUpdatePort.publishAfterCommit(eventId);
        return saved;
    }

    @Transactional
    public Event postpone(UUID eventId, String reason, LocalDateTime newStartAt, LocalDateTime newEndAt) {
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event postponed = event.postpone(newStartAt, newEndAt, reason);

        ensureAssignedMonitorsAvailable(eventId, postponed);
        if (postponed.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        postponed.getStartAt(), postponed.getEndAt(), postponed.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        Event saved = eventPersistencePort.savePostponementRequest(event, postponed, reason);
        attendanceLiveUpdatePort.publishAfterCommit(eventId);
        return saved;
    }

    @Transactional
    public Event confirmPostponement(UUID eventId) {
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Event confirmed = event.confirmPostponement();
        String reason = eventPersistencePort.findPendingPostponementReason(eventId)
                .orElseThrow(() -> new EventPostponementNotAllowedException(
                        "No hay una solicitud de aplazamiento pendiente."));

        ensureAssignedMonitorsAvailable(eventId, confirmed);
        if (confirmed.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        confirmed.getStartAt(), confirmed.getEndAt(), confirmed.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        Event saved = eventPersistencePort.savePostponementConfirmation(event, confirmed, reason);
        attendanceLiveUpdatePort.publishAfterCommit(eventId);
        return saved;
    }

    @Transactional
    public Event update(UUID eventId, UpdateEventCommand command) {
        Event existing = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (command.capacity() < activeRegistrationCountPort.countActiveRegistrations(eventId)) {
            throw new EventModificationNotAllowedException(
                    "El aforo no puede ser inferior al número de inscripciones activas.");
        }
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

        if (!existing.getStartAt().equals(updated.getStartAt())
                || !existing.getEndAt().equals(updated.getEndAt())) {
            ensureAssignedMonitorsAvailable(eventId, updated);
        }
        if (updated.getModality() != Modalidad.VIRTUAL
                && eventPersistencePort.hasScheduleConflict(
                        updated.getStartAt(), updated.getEndAt(), updated.getLocation(), eventId)) {
            throw new EventScheduleConflictException();
        }

        Event saved = eventPersistencePort.save(updated);
        attendanceLiveUpdatePort.publishAfterCommit(eventId);
        return saved;
    }

    private void ensureAssignedMonitorsAvailable(UUID eventId, Event event) {
        monitorSchedulePort.lockAssignedMonitors(eventId);
        monitorSchedulePort.findConflictForEvent(eventId, event.getStartAt(), event.getEndAt())
                .ifPresent(conflict -> {
                    throw new MonitorScheduleConflictException(
                            conflict.monitorId(), conflict.eventId(), conflict.eventName());
                });
    }
}