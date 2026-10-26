package com.syncevent.participants.api;

import java.util.List;
import java.util.UUID;

import com.syncevent.participants.domain.ParticipantHistoryPage;

public record ParticipantHistoryResponse(
        UUID participantId,
        List<ParticipantHistoryEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean certificatesSupported) {

    static ParticipantHistoryResponse from(UUID participantId, ParticipantHistoryPage history) {
        return new ParticipantHistoryResponse(
                participantId,
                history.content().stream().map(ParticipantHistoryEntryResponse::from).toList(),
                history.page(),
                history.size(),
                history.totalElements(),
                history.totalPages(),
                false);
    }
}
