package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.events.domain.port.MonitorScheduleConflict;
import com.syncevent.users.domain.UserRole;

interface SpringDataEventMonitorAssignmentRepository
        extends JpaRepository<EventMonitorAssignmentEntity, EventMonitorAssignmentId> {

    boolean existsByEventIdAndMonitorId(UUID eventId, UUID monitorId);

    List<EventMonitorAssignmentEntity> findAllByEventIdOrderByAssignedAtAscMonitorIdAsc(UUID eventId);

    @Query("""
            SELECT new com.syncevent.events.domain.model.MonitorAvailability(
                monitor.id,
                monitor.email,
                monitor.active,
                CASE WHEN assigned.eventId IS NULL THEN false ELSE true END,
                conflict.id,
                conflict.name)
            FROM UserAccountEntity monitor
            JOIN monitor.roles role
            LEFT JOIN EventMonitorAssignmentEntity assigned ON assigned.monitorId = monitor.id
                AND assigned.eventId = :excludedEventId
            LEFT JOIN EventMonitorAssignmentEntity otherAssignment ON otherAssignment.monitorId = monitor.id
                AND otherAssignment.eventId <> :excludedEventId
            LEFT JOIN EventEntity conflict ON conflict.id = otherAssignment.eventId
                AND conflict.status NOT IN (
                    com.syncevent.domain.model.EstadoEvento.FINALIZADO,
                    com.syncevent.domain.model.EstadoEvento.CANCELADO)
                AND conflict.startAt < :endAt
                AND conflict.endAt > :startAt
            WHERE role = :monitorRole
            ORDER BY monitor.email ASC, monitor.id ASC, conflict.startAt ASC, conflict.id ASC
            """)
    List<com.syncevent.events.domain.model.MonitorAvailability> findAvailability(
            @Param("monitorRole") UserRole monitorRole,
            @Param("excludedEventId") UUID excludedEventId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    @Query("""
            SELECT new com.syncevent.events.domain.port.MonitorScheduleConflict(
                assignment.monitorId, event.id, event.name)
            FROM EventMonitorAssignmentEntity assignment
            JOIN EventEntity event ON event.id = assignment.eventId
            WHERE assignment.monitorId = :monitorId
              AND event.id <> :excludedEventId
              AND event.status NOT IN (
                  com.syncevent.domain.model.EstadoEvento.FINALIZADO,
                  com.syncevent.domain.model.EstadoEvento.CANCELADO)
              AND event.startAt < :endAt
              AND event.endAt > :startAt
            ORDER BY event.startAt ASC, event.id ASC
            """)
    List<MonitorScheduleConflict> findConflicts(
            @Param("monitorId") UUID monitorId,
            @Param("excludedEventId") UUID excludedEventId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    @Query("""
            SELECT new com.syncevent.events.domain.port.MonitorScheduleConflict(
                assignment.monitorId, conflictingEvent.id, conflictingEvent.name)
            FROM EventMonitorAssignmentEntity assignment
            JOIN EventEntity targetEvent ON targetEvent.id = assignment.eventId
            JOIN EventMonitorAssignmentEntity otherAssignment
                ON otherAssignment.monitorId = assignment.monitorId
            JOIN EventEntity conflictingEvent ON conflictingEvent.id = otherAssignment.eventId
            WHERE assignment.eventId = :eventId
              AND conflictingEvent.id <> :eventId
              AND conflictingEvent.status NOT IN (
                  com.syncevent.domain.model.EstadoEvento.FINALIZADO,
                  com.syncevent.domain.model.EstadoEvento.CANCELADO)
              AND conflictingEvent.startAt < :endAt
              AND conflictingEvent.endAt > :startAt
            ORDER BY conflictingEvent.startAt ASC, conflictingEvent.id ASC
            """)
    List<MonitorScheduleConflict> findConflictsForEvent(
            @Param("eventId") UUID eventId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);
}
