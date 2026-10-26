package com.syncevent.attendance.api;

import java.util.List;

import com.syncevent.attendance.domain.FinePage;

public record FinePageResponse(
        List<ParticipantFineResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static FinePageResponse from(FinePage result) {
        int totalPages = result.totalElements() == 0
                ? 0
                : (int) Math.ceil((double) result.totalElements() / result.size());
        return new FinePageResponse(
                result.content().stream().map(ParticipantFineResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                totalPages);
    }
}
