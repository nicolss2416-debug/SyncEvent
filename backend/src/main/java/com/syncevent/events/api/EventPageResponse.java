package com.syncevent.events.api;

import java.util.List;

import com.syncevent.events.domain.port.EventSearchPage;

public record EventPageResponse(
        List<EventResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static EventPageResponse from(EventSearchPage page) {
        int totalPages = page.totalElements() == 0
                ? 0
                : (int) Math.ceil((double) page.totalElements() / page.size());
        return new EventPageResponse(
                page.content().stream().map(EventResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                totalPages);
    }
}
