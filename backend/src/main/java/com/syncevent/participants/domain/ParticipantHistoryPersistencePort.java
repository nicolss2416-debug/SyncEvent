package com.syncevent.participants.domain;

import java.util.UUID;

public interface ParticipantHistoryPersistencePort {

    ParticipantHistoryPage findHistory(UUID participantId, int page, int size);
}
