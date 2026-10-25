package com.syncevent.events.domain.port;

import java.time.LocalDateTime;

import com.syncevent.domain.model.Event;

public interface EventPersistencePort {

    boolean hasScheduleConflict(LocalDateTime startAt, LocalDateTime endAt, String location);

    Event save(Event event);

    EventSearchPage search(EventSearchCriteria criteria);
}