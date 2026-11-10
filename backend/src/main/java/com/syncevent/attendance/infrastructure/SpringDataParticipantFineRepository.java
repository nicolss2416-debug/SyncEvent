package com.syncevent.attendance.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.attendance.domain.FineStatus;

import jakarta.persistence.LockModeType;

public interface SpringDataParticipantFineRepository
        extends JpaRepository<ParticipantFineEntity, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM ParticipantFineEntity f
            WHERE f.triggeredByAbsence.registration.participant.id = :participantId
              AND f.status = :status
            """)
    boolean existsByParticipantIdAndStatus(
            @Param("participantId") UUID participantId,
            @Param("status") FineStatus status);

    @EntityGraph(attributePaths = {
            "triggeredByAbsence",
            "triggeredByAbsence.registration",
            "triggeredByAbsence.registration.participant"
    })
    Page<ParticipantFineEntity> findAllByStatusOrderByCreatedAtDescIdAsc(FineStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "triggeredByAbsence",
            "triggeredByAbsence.registration",
            "triggeredByAbsence.registration.participant"
    })
    Page<ParticipantFineEntity> findAllByOrderByCreatedAtDescIdAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "triggeredByAbsence",
            "triggeredByAbsence.registration",
            "triggeredByAbsence.registration.participant"
    })
    @Query("SELECT f FROM ParticipantFineEntity f WHERE f.id = :fineId")
    Optional<ParticipantFineEntity> findByIdForUpdate(@Param("fineId") UUID fineId);
}
