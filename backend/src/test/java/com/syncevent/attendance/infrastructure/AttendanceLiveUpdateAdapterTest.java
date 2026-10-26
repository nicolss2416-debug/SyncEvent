package com.syncevent.attendance.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import reactor.core.Disposable;

class AttendanceLiveUpdateAdapterTest {

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publicaLaActualizacionSoloDespuesDelCommit() {
        AttendanceLiveUpdateAdapter adapter = new AttendanceLiveUpdateAdapter();
        UUID eventId = UUID.randomUUID();
        List<UUID> updates = new CopyOnWriteArrayList<>();
        List<UUID> secondClientUpdates = new CopyOnWriteArrayList<>();
        Disposable subscription = adapter.updatesFor(eventId).subscribe(updates::add);
        Disposable secondSubscription = adapter.updatesFor(eventId).subscribe(secondClientUpdates::add);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            adapter.publishAfterCommit(eventId);
            assertTrue(updates.isEmpty());

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronizationManager.clearSynchronization();
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            assertEquals(List.of(eventId), updates);
            assertEquals(List.of(eventId), secondClientUpdates);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            subscription.dispose();
            secondSubscription.dispose();
        }
    }
}
