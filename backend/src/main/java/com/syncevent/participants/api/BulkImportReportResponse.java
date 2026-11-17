package com.syncevent.participants.api;

import java.util.List;

public record BulkImportReportResponse(
        int total,
        int saved,
        int rejected,
        List<BulkImportError> errors) {

    public record BulkImportError(
            int row,
            String documentNumber,
            String reason) {
    }
}
