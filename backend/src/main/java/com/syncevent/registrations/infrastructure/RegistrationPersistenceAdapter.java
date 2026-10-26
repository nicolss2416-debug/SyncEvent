package com.syncevent.registrations.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.domain.model.Event;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.registrations.domain.RegistrationStatus;
import java.time.LocalDateTime;

@Repository
public class RegistrationPersistenceAdapter implements RegistrationPersistencePort {

    private final SpringDataRegistrationRepository repository;
    private final SpringDataRegistrationCancellationHistoryRepository cancellationHistoryRepository;

    public RegistrationPersistenceAdapter(
            SpringDataRegistrationRepository repository,
            SpringDataRegistrationCancellationHistoryRepository cancellationHistoryRepository) {
        this.repository = repository;
        this.cancellationHistoryRepository = cancellationHistoryRepository;
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
    public Optional<Registration> findById(UUID registrationId) {
        return repository.findById(registrationId).map(RegistrationEntity::toDomain);
    }

    @Override
    public Optional<Registration> findByIdForUpdate(UUID registrationId) {
        return repository.findByIdForUpdate(registrationId).map(RegistrationEntity::toDomain);
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

    @Override
    public void saveCancellation(
            Registration registration, UUID canceledBy, String reason, LocalDateTime canceledAt) {
        repository.saveAndFlush(RegistrationEntity.fromDomain(registration));
        cancellationHistoryRepository.saveAndFlush(RegistrationCancellationHistoryEntity.create(
                registration.getId(), canceledBy, reason, canceledAt));
    }
}
