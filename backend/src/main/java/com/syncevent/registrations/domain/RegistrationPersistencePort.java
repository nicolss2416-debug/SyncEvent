package com.syncevent.registrations.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.syncevent.events.domain.Event;

public interface RegistrationPersistencePort {

    long countAll();

    long countActiveByEventId(UUID eventId);

    boolean existsActiveByEventIdAndParticipantId(UUID eventId, UUID participantId);

    Optional<Registration> findById(UUID registrationId);

    List<Registration> findAllByEventId(UUID eventId);

    List<Registration> findAllByEventIdAndParticipantId(UUID eventId, UUID participantId);

    Optional<Registration> findByIdForUpdate(UUID registrationId);

    Optional<Event> findScheduleConflict(UUID participantId, UUID excludedEventId, Event targetEvent);

    Registration save(Registration registration);

    void saveCancellation(Registration registration, UUID canceledBy, String reason, java.time.LocalDateTime canceledAt);
}
