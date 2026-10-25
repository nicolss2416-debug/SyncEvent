package com.syncevent.participants.api;

import java.util.List;

import com.syncevent.participants.domain.ParticipantSearchPage;

public record ParticipantPageResponse(
        List<ParticipantResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean empty) {

    public static ParticipantPageResponse from(ParticipantSearchPage result) {
        int totalPages = result.totalElements() == 0
                ? 0
                : (int) Math.ceil((double) result.totalElements() / result.size());
        return new ParticipantPageResponse(
                result.content().stream().map(ParticipantResponse::from).toList(),
                result.totalElements(),
                result.page(),
                result.size(),
                totalPages,
                result.content().isEmpty());
    }
}
