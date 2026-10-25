package com.syncevent.events.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCancellationHistoryRepository
        extends JpaRepository<EventCancellationHistoryEntity, UUID> {
}
