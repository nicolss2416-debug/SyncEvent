package com.syncevent.events.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.events.domain.port.ActiveRegistrationCountPort;
import com.syncevent.events.domain.port.EventSearchCriteria;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.MonitorSchedulePort;
import com.syncevent.events.domain.model.MonitorAssignment;
import com.syncevent.events.domain.model.MonitorAvailability;
import com.syncevent.attendance.application.AttendanceLiveUpdatePort;

import reactor.core.publisher.Flux;
class EventServiceTest {

    @Test
    void noConfirmaAplazamientoSiLaFechaAhoraTieneConflicto() {
        LocalDateTime startAt = LocalDateTime.of(2027, 3, 1, 9, 0);
        Event published = Event.create(
                "Capacitación Java",
                null,
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                startAt,
                startAt.plusHours(3),
                "Sede central",
                null,
                30,
                new BigDecimal("3.00")).transitionTo(EstadoEvento.PUBLICADO);
        Event postponed = published.postpone(
                LocalDateTime.of(2027, 3, 5, 9, 0),
                LocalDateTime.of(2027, 3, 5, 12, 0),
                "Cambio operativo");
        ConflictPersistencePort persistencePort = new ConflictPersistencePort(postponed, true);
        EventService service = new EventService(
                persistencePort, eventId -> 0, eventId -> {}, noMonitorConflicts(), noLiveUpdates());

        assertThrows(EventScheduleConflictException.class,
                () -> service.confirmPostponement(postponed.getId()));

        assertEquals(EstadoEvento.APLAZADO, persistencePort.currentEvent.getStatus());
        assertFalse(persistencePort.confirmationSaved);
    }

    @Test
    void permiteReducirAforoHastaInscritosPeroNoPorDebajo() {
        LocalDateTime startAt = LocalDateTime.of(2027, 3, 1, 9, 0);
        Event event = Event.create(
                "Capacitación Java", null, TipoEvento.CAPACITACION, Modalidad.PRESENCIAL,
                startAt, startAt.plusHours(3), "Sede central", null, 30, new BigDecimal("3.00"));
        ConflictPersistencePort eventPort = new ConflictPersistencePort(event, false);
        ActiveRegistrationCountPort countPort = ignored -> 5;
        EventService service = new EventService(
                eventPort, countPort, eventId -> {}, noMonitorConflicts(), noLiveUpdates());
        UpdateEventCommand allowed = new UpdateEventCommand(
                "Capacitación Java", null, TipoEvento.CAPACITACION, Modalidad.PRESENCIAL,
                startAt, startAt.plusHours(3), "Sede central", null, 5, new BigDecimal("3.00"));

        assertEquals(5, service.update(event.getId(), allowed).getCapacity());

        UpdateEventCommand tooSmall = new UpdateEventCommand(
                "Capacitación Java", null, TipoEvento.CAPACITACION, Modalidad.PRESENCIAL,
                startAt, startAt.plusHours(3), "Sede central", null, 4, new BigDecimal("3.00"));
        assertThrows(com.syncevent.domain.model.EventModificationNotAllowedException.class,
                () -> service.update(event.getId(), tooSmall));
    }

    private static MonitorSchedulePort noMonitorConflicts() {
        return new MonitorSchedulePort() {
            @Override
            public List<MonitorAvailability> findAvailability(
                    UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt) {
                return List.of();
            }

            @Override
            public List<UUID> findAssignedMonitorIds(UUID eventId) {
                return List.of();
            }

            @Override
            public void lockAssignedMonitors(UUID eventId) {
            }

            @Override
            public Optional<com.syncevent.events.domain.port.MonitorScheduleConflict> findConflict(
                    UUID monitorId, UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt) {
                return Optional.empty();
            }

            @Override
            public Optional<com.syncevent.events.domain.port.MonitorScheduleConflict> findConflictForEvent(
                    UUID eventId, LocalDateTime startAt, LocalDateTime endAt) {
                return Optional.empty();
            }

            @Override
            public boolean existsAssignment(UUID eventId, UUID monitorId) {
                return false;
            }

            @Override
            public MonitorAssignment save(MonitorAssignment assignment) {
                return assignment;
            }

            @Override
            public void delete(UUID eventId, UUID monitorId) {
            }
        };
    }

    private static AttendanceLiveUpdatePort noLiveUpdates() {
        return new AttendanceLiveUpdatePort() {
            @Override
            public Flux<UUID> updatesFor(UUID eventId) {
                return Flux.empty();
            }

            @Override
            public void publishAfterCommit(UUID eventId) {
            }
        };
    }

    private static final class ConflictPersistencePort implements EventPersistencePort {

        private Event currentEvent;
        private final boolean scheduleConflict;
        private boolean confirmationSaved;

        private ConflictPersistencePort(Event currentEvent, boolean scheduleConflict) {
            this.currentEvent = currentEvent;
            this.scheduleConflict = scheduleConflict;
        }

        @Override
        public boolean hasScheduleConflict(
                LocalDateTime startAt, LocalDateTime endAt, String location) {
            return false;
        }

        @Override
        public boolean hasScheduleConflict(
                LocalDateTime startAt, LocalDateTime endAt, String location, UUID excludedEventId) {
            return scheduleConflict;
        }

        @Override
        public Event save(Event event) {
            currentEvent = event;
            return event;
        }

        @Override
        public Event saveCancellation(Event event, EstadoEvento previousStatus, String reason) {
            currentEvent = event;
            return event;
        }

        @Override
        public Event savePostponementRequest(Event previousEvent, Event postponedEvent, String reason) {
            currentEvent = postponedEvent;
            return postponedEvent;
        }

        @Override
        public Event savePostponementConfirmation(
                Event postponedEvent, Event confirmedEvent, String reason) {
            confirmationSaved = true;
            currentEvent = confirmedEvent;
            return confirmedEvent;
        }

        @Override
        public Optional<String> findPendingPostponementReason(UUID eventId) {
            return Optional.of("Cambio operativo");
        }

        @Override
        public Optional<Event> findById(UUID id) {
            return Optional.of(currentEvent);
        }

        @Override
        public Optional<Event> findByIdForUpdate(UUID id) {
            return Optional.of(currentEvent);
        }

        @Override
        public EventSearchPage search(EventSearchCriteria criteria) {
            return new EventSearchPage(List.of(), 0, 0, 10);
        }

    }
}