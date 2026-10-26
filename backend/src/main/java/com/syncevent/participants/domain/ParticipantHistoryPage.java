package com.syncevent.participants.domain;

import java.util.List;

public record ParticipantHistoryPage(
        List<ParticipantHistoryEntry> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
