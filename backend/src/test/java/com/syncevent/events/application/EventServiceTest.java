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
        ConflictPersistencePort persistencePort = new ConflictPersistencePort(postponed);
        EventService service = new EventService(persistencePort, eventId -> 0);

        assertThrows(EventScheduleConflictException.class,
                () -> service.confirmPostponement(postponed.getId()));

        assertEquals(EstadoEvento.APLAZADO, persistencePort.currentEvent.getStatus());
        assertFalse(persistencePort.confirmationSaved);
    }

    private static final class ConflictPersistencePort implements EventPersistencePort {

        private Event currentEvent;
        private boolean confirmationSaved;

        private ConflictPersistencePort(Event currentEvent) {
            this.currentEvent = currentEvent;
        }

        @Override
        public boolean hasScheduleConflict(
                LocalDateTime startAt, LocalDateTime endAt, String location) {
            return false;
        }

        @Override
        public boolean hasScheduleConflict(
                LocalDateTime startAt, LocalDateTime endAt, String location, UUID excludedEventId) {
            return true;
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

        @Test
        void permiteReducirAforoHastaInscritosPeroNoPorDebajo() {
            LocalDateTime startAt = LocalDateTime.of(2027, 3, 1, 9, 0);
            Event event = Event.create(
                    "Capacitación Java", null, TipoEvento.CAPACITACION, Modalidad.PRESENCIAL,
                    startAt, startAt.plusHours(3), "Sede central", null, 30, new BigDecimal("3.00"));
            ConflictPersistencePort eventPort = new ConflictPersistencePort(event);
            ActiveRegistrationCountPort countPort = ignored -> 5;
            EventService service = new EventService(eventPort, countPort);
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
    }
}