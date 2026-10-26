package com.syncevent.registrations.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.registrations.domain.RegistrationStatus;

interface SpringDataRegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {

    long countByEvent_IdAndStatus(UUID eventId, RegistrationStatus status);

    boolean existsByEvent_IdAndParticipant_IdAndStatus(
            UUID eventId, UUID participantId, RegistrationStatus status);

    @Query("""
            SELECT r.event FROM RegistrationEntity r
            WHERE r.participant.id = :participantId
              AND r.event.id <> :excludedEventId
              AND r.status = com.syncevent.registrations.domain.RegistrationStatus.ACTIVA
              AND r.event.status IN (
                  com.syncevent.domain.model.EstadoEvento.PUBLICADO,
                  com.syncevent.domain.model.EstadoEvento.EN_CURSO)
              AND r.event.startAt < :targetEndAt
              AND r.event.endAt > :targetStartAt
            ORDER BY r.event.startAt ASC, r.event.id ASC
            """)
    Page<com.syncevent.events.infrastructure.persistence.EventEntity> findScheduleConflicts(
            @Param("participantId") UUID participantId,
            @Param("excludedEventId") UUID excludedEventId,
            @Param("targetStartAt") java.time.LocalDateTime targetStartAt,
            @Param("targetEndAt") java.time.LocalDateTime targetEndAt,
            org.springframework.data.domain.Pageable pageable);
}
