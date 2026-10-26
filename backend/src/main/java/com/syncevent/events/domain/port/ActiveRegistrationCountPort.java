package com.syncevent.events.domain.port;

import java.util.UUID;

public interface ActiveRegistrationCountPort {

    long countActiveRegistrations(UUID eventId);
}
