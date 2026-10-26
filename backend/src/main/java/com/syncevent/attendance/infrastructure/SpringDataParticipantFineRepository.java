package com.syncevent.attendance.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.attendance.domain.FineStatus;

import jakarta.persistence.LockModeType;

public interface SpringDataParticipantFineRepository
        extends JpaRepository<ParticipantFineEntity, UUID> {

    boolean existsByParticipantIdAndStatus(UUID participantId, FineStatus status);

    Page<ParticipantFineEntity> findAllByStatusOrderByCreatedAtDescIdAsc(FineStatus status, Pageable pageable);

    Page<ParticipantFineEntity> findAllByOrderByCreatedAtDescIdAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM ParticipantFineEntity f WHERE f.id = :fineId")
    Optional<ParticipantFineEntity> findByIdForUpdate(@Param("fineId") UUID fineId);
}
