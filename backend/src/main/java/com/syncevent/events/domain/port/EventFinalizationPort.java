package com.syncevent.events.domain.port;

import java.util.UUID;

public interface EventFinalizationPort {

    void processFinalizedEvent(UUID eventId);
}
