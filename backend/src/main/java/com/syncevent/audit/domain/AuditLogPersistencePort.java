package com.syncevent.audit.domain;

public interface AuditLogPersistencePort {

    AuditLog save(AuditLog auditLog);

    AuditLogPage search(AuditLogSearchCriteria criteria);
}
