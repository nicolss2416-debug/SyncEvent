package com.syncevent.registrations.domain;

import java.util.Optional;
import java.util.UUID;

import com.syncevent.domain.model.Event;

public interface RegistrationPersistencePort {

    long countActiveByEventId(UUID eventId);

    boolean existsActiveByEventIdAndParticipantId(UUID eventId, UUID participantId);

    Optional<Event> findScheduleConflict(UUID participantId, UUID excludedEventId, Event targetEvent);

    Registration save(Registration registration);
}
