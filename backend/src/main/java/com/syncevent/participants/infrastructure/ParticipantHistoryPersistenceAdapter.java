package com.syncevent.participants.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.participants.domain.ParticipantHistoryEntry;
import com.syncevent.participants.domain.ParticipantHistoryPage;
import com.syncevent.participants.domain.ParticipantHistoryPersistencePort;

@Repository
public class ParticipantHistoryPersistenceAdapter implements ParticipantHistoryPersistencePort {

    private final SpringDataParticipantHistoryRepository repository;

    public ParticipantHistoryPersistenceAdapter(SpringDataParticipantHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public ParticipantHistoryPage findHistory(UUID participantId, int page, int size) {
        Page<ParticipantHistoryEntry> result = repository.findHistory(
                participantId,
                PageRequest.of(page, size));
        return new ParticipantHistoryPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
