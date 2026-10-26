package com.syncevent.attendance.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.ActiveRegistrationCountPort;
import com.syncevent.events.domain.port.EventPersistencePort;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Service
public class AttendanceLiveStatsService {

    private static final double CAPACITY_ALERT_THRESHOLD = 90.0;

    private final EventPersistencePort eventPersistencePort;
    private final ActiveRegistrationCountPort registrationCountPort;
    private final AttendanceLiveMetricsPort metricsPort;
    private final AttendanceLiveUpdatePort updatePort;

    public AttendanceLiveStatsService(
            EventPersistencePort eventPersistencePort,
            ActiveRegistrationCountPort registrationCountPort,
            AttendanceLiveMetricsPort metricsPort,
            AttendanceLiveUpdatePort updatePort) {
        this.eventPersistencePort = eventPersistencePort;
        this.registrationCountPort = registrationCountPort;
        this.metricsPort = metricsPort;
        this.updatePort = updatePort;
    }

    public Flux<AttendanceLiveStats> subscribe(UUID eventId) {
        return Flux.create(emitter -> {
            Disposable updates = updatePort.updatesFor(eventId)
                    .subscribe(ignored -> emitCurrentStats(eventId, emitter), emitter::error);
            emitter.onDispose(updates::dispose);
            try {
                AttendanceLiveStats initial = getCurrentStats(eventId);
                emitter.next(initial);
                if (initial.eventStatus() != EstadoEvento.EN_CURSO) {
                    emitter.complete();
                }
            } catch (RuntimeException exception) {
                emitter.error(exception);
            }
        });
    }

    @Transactional(readOnly = true)
    public AttendanceLiveStats getCurrentStats(UUID eventId) {
        return currentStats(eventId);
    }

    private void emitCurrentStats(
            UUID eventId, reactor.core.publisher.FluxSink<AttendanceLiveStats> emitter) {
        try {
            AttendanceLiveStats current = currentStats(eventId);
            emitter.next(current);
            if (current.eventStatus() != EstadoEvento.EN_CURSO) {
                emitter.complete();
            }
        } catch (RuntimeException exception) {
            emitter.error(exception);
        }
    }

    private AttendanceLiveStats currentStats(UUID eventId) {
        Event event = eventPersistencePort.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        long checkedInCount = metricsPort.countCheckIns(eventId);
        long activeRegistrations = registrationCountPort.countActiveRegistrations(eventId);
        double occupancy = checkedInCount * 100.0 / event.getCapacity();
        return new AttendanceLiveStats(
                eventId,
                event.getStatus(),
                checkedInCount,
                activeRegistrations,
                event.getCapacity(),
                occupancy,
                occupancy >= CAPACITY_ALERT_THRESHOLD);
    }
}
