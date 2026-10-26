package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.domain.model.EstadoEvento;

import jakarta.persistence.LockModeType;

public interface SpringDataEventRepository
        extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {

    boolean existsByLocationAndStartAtLessThanAndEndAtGreaterThanAndStatusNotAndStatusNot(
            String location,
            LocalDateTime endAt,
            LocalDateTime startAt,
            EstadoEvento excludedStatusOne,
            EstadoEvento excludedStatusTwo);

    boolean existsByLocationAndStartAtLessThanAndEndAtGreaterThanAndStatusNotAndStatusNotAndIdNot(
            String location,
            LocalDateTime endAt,
            LocalDateTime startAt,
            EstadoEvento excludedStatusOne,
            EstadoEvento excludedStatusTwo,
            UUID excludedEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventEntity e WHERE e.id = :id")
    java.util.Optional<EventEntity> findByIdForUpdate(@Param("id") UUID id);
}