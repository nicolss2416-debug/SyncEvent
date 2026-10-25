package com.syncevent.events.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventPostponementHistoryRepository
        extends JpaRepository<EventPostponementHistoryEntity, Long> {

    Optional<EventPostponementHistoryEntity> findFirstByEventIdOrderByIdDesc(UUID eventId);
}
