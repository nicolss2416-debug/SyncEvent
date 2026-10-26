package com.syncevent.users.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuthActivityPersistencePort {

    void record(UUID userId, String email, String activity, LocalDateTime occurredAt);
}
