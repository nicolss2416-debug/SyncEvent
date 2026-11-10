package com.syncevent.audit.api;

import java.util.List;

import com.syncevent.audit.domain.AuditLogPage;

public record AuditLogPageResponse(List<AuditLogResponse> items, long total, int page, int size) {

    public static AuditLogPageResponse from(AuditLogPage result) {
        return new AuditLogPageResponse(
                result.items().stream().map(AuditLogResponse::from).toList(),
                result.total(),
                result.page(),
                result.size());
    }
}
