package com.syncevent.audit.domain;

import java.time.LocalDateTime;

public record AuditLogSearchCriteria(
        String userEmail,
        String entityType,
        String action,
        String result,
        LocalDateTime from,
        LocalDateTime to,
        Integer lastHours,
        int page,
        int size) {
}
