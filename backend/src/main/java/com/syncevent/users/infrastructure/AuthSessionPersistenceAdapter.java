package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.AuthSessionPersistencePort;

@Repository
public class AuthSessionPersistenceAdapter implements AuthSessionPersistencePort {

    private final SpringDataAuthSessionRepository repository;

    public AuthSessionPersistenceAdapter(SpringDataAuthSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void create(UUID sessionId, UUID userId, LocalDateTime createdAt) {
        repository.saveAndFlush(AuthSessionEntity.create(sessionId, userId, createdAt));
    }

    @Override
    @Transactional
    public boolean recordActivityIfValid(UUID sessionId, LocalDateTime cutoff, LocalDateTime activityAt) {
        return repository.updateActivityIfValid(sessionId, cutoff, activityAt) == 1;
    }

    @Override
    @Transactional
    public void revoke(UUID sessionId, LocalDateTime revokedAt) {
        repository.revoke(sessionId, revokedAt);
    }
}
