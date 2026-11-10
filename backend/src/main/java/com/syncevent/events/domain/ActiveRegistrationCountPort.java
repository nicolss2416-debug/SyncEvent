package com.syncevent.events.domain;

import java.util.UUID;

public interface ActiveRegistrationCountPort {

    long countActiveRegistrations(UUID eventId);
}
