package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuthSessionPersistencePort {

    void create(UUID sessionId, UUID userId, LocalDateTime createdAt);

    boolean recordActivityIfValid(UUID sessionId, LocalDateTime cutoff, LocalDateTime activityAt);

    void revoke(UUID sessionId, LocalDateTime revokedAt);
}
