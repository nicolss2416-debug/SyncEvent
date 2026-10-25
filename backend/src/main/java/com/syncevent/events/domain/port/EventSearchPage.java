package com.syncevent.events.domain.port;

import java.util.List;

import com.syncevent.domain.model.Event;

public record EventSearchPage(List<Event> content, long totalElements, int page, int size) {
}
