package com.syncevent.attendance.domain;

import java.util.UUID;

import reactor.core.publisher.Flux;

public interface AttendanceLiveUpdatePort {

    Flux<UUID> updatesFor(UUID eventId);

    void publishAfterCommit(UUID eventId);
}
