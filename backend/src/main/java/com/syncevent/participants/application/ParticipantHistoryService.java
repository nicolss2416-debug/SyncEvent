package com.syncevent.participants.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.participants.domain.ParticipantHistoryPage;
import com.syncevent.participants.domain.ParticipantHistoryPersistencePort;
import com.syncevent.participants.domain.ParticipantPersistencePort;

@Service
public class ParticipantHistoryService {

    private final ParticipantPersistencePort participantPersistencePort;
    private final ParticipantHistoryPersistencePort historyPersistencePort;

    public ParticipantHistoryService(
            ParticipantPersistencePort participantPersistencePort,
            ParticipantHistoryPersistencePort historyPersistencePort) {
        this.participantPersistencePort = participantPersistencePort;
        this.historyPersistencePort = historyPersistencePort;
    }

    @Transactional(readOnly = true)
    public ParticipantHistoryPage findHistory(UUID participantId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("La página debe ser positiva y el tamaño debe estar entre 1 y 100.");
        }
        participantPersistencePort.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
        return historyPersistencePort.findHistory(participantId, page, size);
    }
}
