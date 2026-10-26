package com.syncevent.registrations.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.domain.model.Event;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.registrations.domain.RegistrationStatus;

@Repository
public class RegistrationPersistenceAdapter implements RegistrationPersistencePort {

    private final SpringDataRegistrationRepository repository;

    public RegistrationPersistenceAdapter(SpringDataRegistrationRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countActiveByEventId(UUID eventId) {
        return repository.countByEvent_IdAndStatus(eventId, RegistrationStatus.ACTIVA);
    }

    @Override
    public boolean existsActiveByEventIdAndParticipantId(UUID eventId, UUID participantId) {
        return repository.existsByEvent_IdAndParticipant_IdAndStatus(
                eventId, participantId, RegistrationStatus.ACTIVA);
    }

    @Override
    public Optional<Event> findScheduleConflict(UUID participantId, UUID excludedEventId, Event targetEvent) {
        return repository.findScheduleConflicts(
                        participantId,
                        excludedEventId,
                        targetEvent.getStartAt(),
                        targetEvent.getEndAt(),
                        PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(eventEntity -> eventEntity.toDomain());
    }

    @Override
    public Registration save(Registration registration) {
        return repository.saveAndFlush(RegistrationEntity.fromDomain(registration)).toDomain();
    }
}
