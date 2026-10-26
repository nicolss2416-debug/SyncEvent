package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.events.domain.model.MonitorAssignment;
import com.syncevent.events.domain.model.MonitorAvailability;
import com.syncevent.events.domain.port.MonitorScheduleConflict;
import com.syncevent.events.domain.port.MonitorSchedulePort;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@Repository
public class MonitorSchedulePersistenceAdapter implements MonitorSchedulePort {

    private final SpringDataEventMonitorAssignmentRepository repository;
    private final UserPersistencePort userPersistencePort;

    public MonitorSchedulePersistenceAdapter(
            SpringDataEventMonitorAssignmentRepository repository,
            UserPersistencePort userPersistencePort) {
        this.repository = repository;
        this.userPersistencePort = userPersistencePort;
    }

    @Override
    public List<MonitorAvailability> findAvailability(
            UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt) {
        List<MonitorAvailability> rows = repository.findAvailability(
                UserRole.MONITOR, excludedEventId, startAt, endAt);
        java.util.Map<UUID, MonitorAvailability> availability = new java.util.LinkedHashMap<>();
        for (MonitorAvailability row : rows) {
            MonitorAvailability current = availability.get(row.monitorId());
            if (current == null || current.conflictingEventId() == null) {
                availability.put(row.monitorId(), row);
            }
        }
        return List.copyOf(availability.values());
    }

    @Override
    public List<UUID> findAssignedMonitorIds(UUID eventId) {
        return repository.findAllByEventIdOrderByAssignedAtAscMonitorIdAsc(eventId).stream()
                .map(EventMonitorAssignmentEntity::toDomain)
                .map(MonitorAssignment::monitorId)
                .toList();
    }

    @Override
    public void lockAssignedMonitors(UUID eventId) {
        findAssignedMonitorIds(eventId).stream()
                .sorted()
                .forEach(monitorId -> userPersistencePort.findByIdForUpdate(monitorId)
                        .orElseThrow(() -> new IllegalStateException(
                                "La asignación referencia una cuenta de monitor inexistente.")));
    }

    @Override
    public Optional<MonitorScheduleConflict> findConflict(
            UUID monitorId, UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt) {
        return repository.findConflicts(monitorId, excludedEventId, startAt, endAt).stream().findFirst();
    }

    @Override
    public Optional<MonitorScheduleConflict> findConflictForEvent(
            UUID eventId, LocalDateTime startAt, LocalDateTime endAt) {
        return repository.findConflictsForEvent(eventId, startAt, endAt).stream().findFirst();
    }

    @Override
    public boolean existsAssignment(UUID eventId, UUID monitorId) {
        return repository.existsByEventIdAndMonitorId(eventId, monitorId);
    }

    @Override
    public MonitorAssignment save(MonitorAssignment assignment) {
        return repository.saveAndFlush(EventMonitorAssignmentEntity.fromDomain(assignment)).toDomain();
    }

    @Override
    public void delete(UUID eventId, UUID monitorId) {
        repository.deleteById(new EventMonitorAssignmentId(eventId, monitorId));
    }
}
