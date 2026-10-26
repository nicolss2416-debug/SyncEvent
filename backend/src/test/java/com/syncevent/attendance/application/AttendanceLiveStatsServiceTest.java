package com.syncevent.attendance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;
import com.syncevent.events.domain.port.ActiveRegistrationCountPort;
import com.syncevent.events.domain.port.EventPersistencePort;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

class AttendanceLiveStatsServiceTest {

    @Test
    void emitePrimeraFotoYActualizaEstadisticasCuandoSeRegistraAsistencia() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId, EstadoEvento.EN_CURSO, 10);
        EventPersistencePort eventPort = mock(EventPersistencePort.class);
        ActiveRegistrationCountPort registrationCountPort = mock(ActiveRegistrationCountPort.class);
        AttendanceLiveMetricsPort metricsPort = mock(AttendanceLiveMetricsPort.class);
        AttendanceLiveUpdatePort updatePort = mock(AttendanceLiveUpdatePort.class);
        Sinks.Many<UUID> updates = Sinks.many().multicast().directBestEffort();
        when(eventPort.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationCountPort.countActiveRegistrations(eventId)).thenReturn(10L);
        when(metricsPort.countCheckIns(eventId)).thenReturn(0L, 9L);
        when(updatePort.updatesFor(eventId)).thenReturn(updates.asFlux());
        AttendanceLiveStatsService service = new AttendanceLiveStatsService(
                eventPort, registrationCountPort, metricsPort, updatePort);

        var received = new CopyOnWriteArrayList<AttendanceLiveStats>();
        CountDownLatch initialUpdate = new CountDownLatch(1);
        CountDownLatch refreshedUpdate = new CountDownLatch(1);
        Disposable subscription = service.subscribe(eventId).subscribe(stats -> {
            received.add(stats);
            if (received.size() == 1) {
                initialUpdate.countDown();
            } else {
                refreshedUpdate.countDown();
            }
        });
        try {
            assertTrue(initialUpdate.await(2, TimeUnit.SECONDS));
            assertEquals(0L, received.get(0).checkedInCount());
            assertEquals(0.0, received.get(0).occupancyPercentage());

            updates.tryEmitNext(eventId);
            assertTrue(refreshedUpdate.await(2, TimeUnit.SECONDS));
            assertEquals(9L, received.get(1).checkedInCount());
            assertEquals(90.0, received.get(1).occupancyPercentage());
            assertTrue(received.get(1).capacityAlert());
            assertEquals(10L, received.get(1).activeRegistrationCount());
        } finally {
            subscription.dispose();
        }
    }

    @Test
    void difundeLaActualizacionATodosLosSuscriptoresActivos() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId, EstadoEvento.EN_CURSO, 10);
        EventPersistencePort eventPort = mock(EventPersistencePort.class);
        ActiveRegistrationCountPort registrationCountPort = mock(ActiveRegistrationCountPort.class);
        AttendanceLiveMetricsPort metricsPort = mock(AttendanceLiveMetricsPort.class);
        AttendanceLiveUpdatePort updatePort = mock(AttendanceLiveUpdatePort.class);
        Sinks.Many<UUID> updates = Sinks.many().multicast().directBestEffort();
        AtomicBoolean refreshed = new AtomicBoolean();
        when(eventPort.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationCountPort.countActiveRegistrations(eventId)).thenReturn(10L);
        when(metricsPort.countCheckIns(eventId)).thenAnswer(invocation -> refreshed.get() ? 9L : 0L);
        when(updatePort.updatesFor(eventId)).thenReturn(updates.asFlux());
        AttendanceLiveStatsService service = new AttendanceLiveStatsService(
                eventPort, registrationCountPort, metricsPort, updatePort);

        var firstClient = new CopyOnWriteArrayList<AttendanceLiveStats>();
        var secondClient = new CopyOnWriteArrayList<AttendanceLiveStats>();
        CountDownLatch initialUpdates = new CountDownLatch(2);
        CountDownLatch refreshedUpdates = new CountDownLatch(2);
        Disposable firstSubscription = service.subscribe(eventId).subscribe(stats -> {
            firstClient.add(stats);
            (firstClient.size() == 1 ? initialUpdates : refreshedUpdates).countDown();
        });
        Disposable secondSubscription = service.subscribe(eventId).subscribe(stats -> {
            secondClient.add(stats);
            (secondClient.size() == 1 ? initialUpdates : refreshedUpdates).countDown();
        });
        try {
            assertTrue(initialUpdates.await(2, TimeUnit.SECONDS));
            refreshed.set(true);
            updates.tryEmitNext(eventId);

            assertTrue(refreshedUpdates.await(2, TimeUnit.SECONDS));
            assertEquals(2, firstClient.size());
            assertEquals(2, secondClient.size());
            assertEquals(9L, firstClient.get(1).checkedInCount());
            assertEquals(9L, secondClient.get(1).checkedInCount());
            assertTrue(firstClient.get(1).capacityAlert());
            assertTrue(secondClient.get(1).capacityAlert());
        } finally {
            firstSubscription.dispose();
            secondSubscription.dispose();
        }
    }

    @Test
    void emiteEstadoYFinalizaLaSuscripcionSiElEventoNoEstaEnCurso() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId, EstadoEvento.PUBLICADO, 10);
        EventPersistencePort eventPort = mock(EventPersistencePort.class);
        ActiveRegistrationCountPort registrationCountPort = mock(ActiveRegistrationCountPort.class);
        AttendanceLiveMetricsPort metricsPort = mock(AttendanceLiveMetricsPort.class);
        AttendanceLiveUpdatePort updatePort = mock(AttendanceLiveUpdatePort.class);
        when(eventPort.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationCountPort.countActiveRegistrations(eventId)).thenReturn(0L);
        when(metricsPort.countCheckIns(eventId)).thenReturn(0L);
        when(updatePort.updatesFor(eventId)).thenReturn(Flux.empty());
        AttendanceLiveStatsService service = new AttendanceLiveStatsService(
                eventPort, registrationCountPort, metricsPort, updatePort);

        var stats = service.subscribe(eventId).collectList().block(Duration.ofSeconds(1));

        assertEquals(1, stats.size());
        assertEquals(EstadoEvento.PUBLICADO, stats.get(0).eventStatus());
        assertEquals(0L, stats.get(0).checkedInCount());
        assertFalse(stats.get(0).capacityAlert());
    }

    private static Event event(UUID id, EstadoEvento status, int capacity) {
        LocalDateTime startAt = LocalDateTime.of(2027, 6, 1, 9, 0);
        return new Event(
                id,
                "Evento de prueba",
                null,
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                startAt,
                startAt.plusHours(3),
                "Sede central",
                null,
                capacity,
                new BigDecimal("3.00"),
                status,
                startAt.minusDays(1),
                startAt.minusDays(1));
    }
}
