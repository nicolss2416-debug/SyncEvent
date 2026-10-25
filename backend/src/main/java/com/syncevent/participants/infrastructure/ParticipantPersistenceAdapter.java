package com.syncevent.participants.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.participants.domain.ParticipantSearchPage;

@Component
public class ParticipantPersistenceAdapter implements ParticipantPersistencePort {

    private final SpringDataParticipantRepository repository;
    private final ParticipantChangeHistoryRepository historyRepository;

    public ParticipantPersistenceAdapter(
            SpringDataParticipantRepository repository,
            ParticipantChangeHistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    @Override
    public Optional<Participant> findByDocument(String documentType, String documentNumber) {
        return repository.findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                .map(ParticipantEntity::toDomain);
    }

    @Override
    public Optional<Participant> findById(UUID id) {
        return repository.findById(id).map(ParticipantEntity::toDomain);
    }

    @Override
    public ParticipantSearchPage search(String query, int page, int size) {
        Page<ParticipantEntity> result = repository.search(
                query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName", "id")));
        return new ParticipantSearchPage(
                result.getContent().stream().map(ParticipantEntity::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }

    @Override
    public Participant save(Participant participant) {
        return repository.save(ParticipantEntity.fromDomain(participant)).toDomain();
    }

    @Override
    public Participant saveCorrection(Participant participant, String changedBy) {
        Participant saved = repository.save(ParticipantEntity.fromDomain(participant)).toDomain();
        historyRepository.save(ParticipantChangeHistoryEntity.record(
                saved.getId(), changedBy, saved.getUpdatedAt()));
        return saved;
    }
}
