package com.syncevent.registrations.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRegistrationCancellationHistoryRepository
        extends JpaRepository<RegistrationCancellationHistoryEntity, UUID> {
}
