package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.users.domain.AuthActivityPersistencePort;

@Repository
public class AuthActivityPersistenceAdapter implements AuthActivityPersistencePort {

    private final SpringDataAuthActivityRepository repository;

    public AuthActivityPersistenceAdapter(SpringDataAuthActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(UUID userId, String email, String activity, LocalDateTime occurredAt) {
        repository.save(AuthActivityEntity.create(userId, email, activity, occurredAt));
    }
}
