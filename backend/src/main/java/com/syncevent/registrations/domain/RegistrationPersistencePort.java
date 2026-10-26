package com.syncevent.registrations.domain;

import java.util.Optional;
import java.util.UUID;

import com.syncevent.domain.model.Event;

public interface RegistrationPersistencePort {

    long countActiveByEventId(UUID eventId);

    boolean existsActiveByEventIdAndParticipantId(UUID eventId, UUID participantId);

    Optional<Registration> findById(UUID registrationId);

    Optional<Registration> findByIdForUpdate(UUID registrationId);

    Optional<Event> findScheduleConflict(UUID participantId, UUID excludedEventId, Event targetEvent);

    Registration save(Registration registration);

    void saveCancellation(Registration registration, UUID canceledBy, String reason, java.time.LocalDateTime canceledAt);
}
