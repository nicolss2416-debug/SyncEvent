package com.syncevent.participants.application;

import java.util.List;

import com.syncevent.participants.domain.Participant;

public record BulkImportResult(
        List<Participant> saved,
        List<RowError> errors) {

    public record RowError(int row, String documentNumber, String reason) {
    }
}
