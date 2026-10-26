package com.syncevent.events.domain.port;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.EstadoEvento;

public interface EventPersistencePort {

    boolean hasScheduleConflict(LocalDateTime startAt, LocalDateTime endAt, String location);

    boolean hasScheduleConflict(
            LocalDateTime startAt, LocalDateTime endAt, String location, java.util.UUID excludedEventId);

    Event save(Event event);

    Event saveCancellation(Event event, EstadoEvento previousStatus, String reason);

    Event savePostponementRequest(Event previousEvent, Event postponedEvent, String reason);

    Event savePostponementConfirmation(Event postponedEvent, Event confirmedEvent, String reason);

    Optional<String> findPendingPostponementReason(UUID eventId);

    Optional<Event> findById(java.util.UUID id);

    Optional<Event> findByIdForUpdate(UUID id);

    EventSearchPage search(EventSearchCriteria criteria);
}