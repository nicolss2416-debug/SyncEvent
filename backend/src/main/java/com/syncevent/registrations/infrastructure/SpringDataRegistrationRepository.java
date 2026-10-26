package com.syncevent.registrations.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.registrations.domain.RegistrationStatus;

import jakarta.persistence.LockModeType;

interface SpringDataRegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {

    long countByEvent_IdAndStatus(UUID eventId, RegistrationStatus status);

    boolean existsByEvent_IdAndParticipant_IdAndStatus(
            UUID eventId, UUID participantId, RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationEntity r WHERE r.qrToken = :qrToken")
    java.util.Optional<RegistrationEntity> findByQrTokenForUpdate(@Param("qrToken") String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM RegistrationEntity r
            WHERE r.event.id = :eventId
              AND r.participant.id = :participantId
              AND r.status = com.syncevent.registrations.domain.RegistrationStatus.ACTIVA
            """)
    java.util.Optional<RegistrationEntity> findActiveByEventAndParticipantForUpdate(
            @Param("eventId") UUID eventId,
            @Param("participantId") UUID participantId);

    @Query("""
            SELECT r FROM RegistrationEntity r
            WHERE r.event.id = :eventId
              AND r.status = com.syncevent.registrations.domain.RegistrationStatus.ACTIVA
              AND (
                  LOWER(r.participant.documentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(r.participant.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(r.participant.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(CONCAT(CONCAT(r.participant.firstName, ' '), r.participant.lastName))
                      LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY r.participant.lastName ASC, r.participant.firstName ASC, r.participant.id ASC
            """)
    Page<RegistrationEntity> searchActiveByEvent(
            @Param("eventId") UUID eventId,
            @Param("query") String query,
            org.springframework.data.domain.Pageable pageable);

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
