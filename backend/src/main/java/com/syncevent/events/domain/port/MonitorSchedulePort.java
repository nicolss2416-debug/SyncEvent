package com.syncevent.events.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.syncevent.events.domain.model.MonitorAvailability;
import com.syncevent.events.domain.model.MonitorAssignment;

public interface MonitorSchedulePort {

    List<MonitorAvailability> findAvailability(
            UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt);

    List<UUID> findAssignedMonitorIds(UUID eventId);

    void lockAssignedMonitors(UUID eventId);

    Optional<MonitorScheduleConflict> findConflict(
            UUID monitorId, UUID excludedEventId, LocalDateTime startAt, LocalDateTime endAt);

    Optional<MonitorScheduleConflict> findConflictForEvent(
            UUID eventId, LocalDateTime startAt, LocalDateTime endAt);

    boolean existsAssignment(UUID eventId, UUID monitorId);

    MonitorAssignment save(MonitorAssignment assignment);

    void delete(UUID eventId, UUID monitorId);
}
