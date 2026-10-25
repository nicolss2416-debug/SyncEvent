package com.syncevent.participants.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantChangeHistoryRepository
        extends JpaRepository<ParticipantChangeHistoryEntity, Long> {
}
