package com.syncevent.audit.domain;

import java.util.List;

public record AuditLogPage(List<AuditLog> items, long total, int page, int size) {
}
