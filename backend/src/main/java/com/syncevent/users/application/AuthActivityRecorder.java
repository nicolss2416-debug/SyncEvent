package com.syncevent.users.application;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.AuthActivityPersistencePort;

@Service
public class AuthActivityRecorder {

    private final AuthActivityPersistencePort persistencePort;

    public AuthActivityRecorder(AuthActivityPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Transactional
    public void record(UUID userId, String email, String activity, LocalDateTime occurredAt) {
        persistencePort.record(userId, email, activity, occurredAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String email, LocalDateTime occurredAt) {
        persistencePort.record(null, email, "LOGIN_FAILED", occurredAt);
    }
}
