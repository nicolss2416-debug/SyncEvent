package com.syncevent.participants.domain;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantPersistencePort {

    Optional<Participant> findByDocument(String documentType, String documentNumber);

    Optional<Participant> findById(UUID id);

    ParticipantSearchPage search(String query, int page, int size);

    Participant save(Participant participant);

    Participant saveCorrection(Participant participant, String changedBy);
}
