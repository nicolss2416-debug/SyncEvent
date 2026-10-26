package com.syncevent.events.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.Event;
import com.syncevent.events.domain.model.MonitorAssignment;
import com.syncevent.events.domain.model.MonitorAvailability;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.events.domain.port.MonitorSchedulePort;
import com.syncevent.users.application.AuthActivityRecorder;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@Service
public class MonitorAssignmentService {

    private final EventPersistencePort eventPersistencePort;
    private final UserPersistencePort userPersistencePort;
    private final MonitorSchedulePort monitorSchedulePort;
    private final AuthActivityRecorder activityRecorder;
    private final Clock clock;

    public MonitorAssignmentService(
            EventPersistencePort eventPersistencePort,
            UserPersistencePort userPersistencePort,
            MonitorSchedulePort monitorSchedulePort,
            AuthActivityRecorder activityRecorder,
            Clock clock) {
        this.eventPersistencePort = eventPersistencePort;
        this.userPersistencePort = userPersistencePort;
        this.monitorSchedulePort = monitorSchedulePort;
        this.activityRecorder = activityRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MonitorAvailability> findAvailability(UUID eventId) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return monitorSchedulePort.findAvailability(eventId, event.getStartAt(), event.getEndAt());
    }

    @Transactional
    public MonitorAssignment assign(UUID eventId, UUID monitorId, UUID assignedBy, String actorEmail) {
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        UserAccount monitor = userPersistencePort.findByIdForUpdate(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(monitorId));
        if (!monitor.isActive() || !monitor.getRoles().contains(UserRole.MONITOR)) {
            throw new MonitorAssignmentNotAllowedException(
                    "Solo se pueden asignar cuentas activas con el rol MONITOR.");
        }
        if (monitorSchedulePort.existsAssignment(eventId, monitorId)) {
            throw new MonitorAssignmentNotAllowedException("El monitor ya está asignado a este evento.");
        }
        monitorSchedulePort.findConflict(
                        monitorId, eventId, event.getStartAt(), event.getEndAt())
                .ifPresent(conflict -> {
                    throw new MonitorScheduleConflictException(
                            monitorId, conflict.eventId(), conflict.eventName());
                });

        LocalDateTime assignedAt = LocalDateTime.now(clock);
        MonitorAssignment assignment = monitorSchedulePort.save(
                new MonitorAssignment(eventId, monitorId, assignedBy, assignedAt));
        activityRecorder.record(assignedBy, actorEmail, "MONITOR_ASSIGNED", assignedAt);
        return assignment;
    }

    @Transactional
    public void remove(UUID eventId, UUID monitorId, UUID removedBy, String actorEmail) {
        eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        userPersistencePort.findByIdForUpdate(monitorId)
                .orElseThrow(() -> new MonitorNotFoundException(monitorId));
        if (!monitorSchedulePort.existsAssignment(eventId, monitorId)) {
            throw new MonitorAssignmentNotFoundException(eventId, monitorId);
        }
        monitorSchedulePort.delete(eventId, monitorId);
        activityRecorder.record(removedBy, actorEmail, "MONITOR_REMOVED", LocalDateTime.now(clock));
    }

}
