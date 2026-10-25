package com.syncevent.participants.application;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.participants.domain.ParticipantSearchPage;

@Service
public class ParticipantService {

    private final ParticipantPersistencePort persistencePort;

    public ParticipantService(ParticipantPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Transactional
    public Participant register(RegisterParticipantCommand command) {
        String documentType = command.documentType().trim().toUpperCase(Locale.ROOT);
        String documentNumber = command.documentNumber().trim().toUpperCase(Locale.ROOT);

        persistencePort.findByDocument(documentType, documentNumber)
                .ifPresent(existing -> {
                    throw new DuplicateParticipantException(existing.getId());
                });

        return persistencePort.save(Participant.register(
                documentType,
                documentNumber,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.createdBy()));
    }

    @Transactional(readOnly = true)
    public ParticipantSearchPage search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("El criterio de búsqueda no puede estar vacío.");
        }
        return persistencePort.search(query.trim(), page, size);
    }

    @Transactional(readOnly = true)
    public Participant findById(UUID id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ParticipantNotFoundException(id));
    }

    @Transactional
    public Participant update(UUID id, UpdateParticipantCommand command) {
        Participant existing = findById(id);
        String documentType = command.documentType().trim().toUpperCase(Locale.ROOT);
        String documentNumber = command.documentNumber().trim().toUpperCase(Locale.ROOT);

        persistencePort.findByDocument(documentType, documentNumber)
                .filter(duplicate -> !duplicate.getId().equals(id))
                .ifPresent(duplicate -> {
                    throw new DuplicateParticipantException(duplicate.getId());
                });

        Participant updated = existing.update(
                documentType,
                documentNumber,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.phone(),
                command.updatedBy());
        return persistencePort.saveCorrection(updated, command.updatedBy());
    }
}
