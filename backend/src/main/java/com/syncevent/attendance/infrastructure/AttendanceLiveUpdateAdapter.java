package com.syncevent.attendance.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncevent.attendance.application.AttendanceLiveUpdatePort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AttendanceLiveUpdateAdapter implements AttendanceLiveUpdatePort {

    private final Sinks.Many<UUID> updates = Sinks.many().multicast().directBestEffort();

    @Override
    public Flux<UUID> updatesFor(UUID eventId) {
        return updates.asFlux().filter(updatedEventId -> updatedEventId.equals(eventId));
    }

    @Override
    public void publishAfterCommit(UUID eventId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(eventId);
                }
            });
            return;
        }
        publish(eventId);
    }

    private void publish(UUID eventId) {
        Sinks.EmitResult result;
        synchronized (updates) {
            result = updates.tryEmitNext(eventId);
        }
        if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            return;
        }
        if (result.isFailure()) {
            throw new IllegalStateException(
                    "No se pudo notificar la actualización de asistencia: " + result);
        }
    }
}
