package com.syncevent.events.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.syncevent.events.domain.EventSearchPage;

public record EventPageResponse(
        List<EventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static EventPageResponse from(EventSearchPage page) {
        return from(page, Map.of());
    }

    public static EventPageResponse from(
            EventSearchPage page, Map<UUID, List<String>> monitorsByEvent) {
        int totalPages = page.totalElements() == 0
                ? 0
                : (int) Math.ceil((double) page.totalElements() / page.size());
        return new EventPageResponse(
                page.content().stream()
                        .map(event -> EventResponse.from(
                                event, monitorsByEvent.getOrDefault(event.getId(), List.of())))
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                totalPages);
    }
}
