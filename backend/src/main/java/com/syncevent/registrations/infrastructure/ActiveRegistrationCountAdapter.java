package com.syncevent.registrations.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.events.domain.port.ActiveRegistrationCountPort;
import com.syncevent.registrations.domain.RegistrationPersistencePort;

@Repository
public class ActiveRegistrationCountAdapter implements ActiveRegistrationCountPort {

    private final RegistrationPersistencePort registrationPersistencePort;

    public ActiveRegistrationCountAdapter(RegistrationPersistencePort registrationPersistencePort) {
        this.registrationPersistencePort = registrationPersistencePort;
    }

    @Override
    public long countActiveRegistrations(UUID eventId) {
        return registrationPersistencePort.countActiveByEventId(eventId);
    }
}
