package com.syncevent.events.domain;

import java.util.UUID;

public interface EventFinalizationPort {

    void processFinalizedEvent(UUID eventId);
}
