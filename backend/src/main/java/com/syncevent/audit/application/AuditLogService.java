package com.syncevent.audit.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.audit.domain.AuditLog;
import com.syncevent.audit.domain.AuditLogPage;
import com.syncevent.audit.domain.AuditLogPersistencePort;
import com.syncevent.audit.domain.AuditLogSearchCriteria;

@Service
public class AuditLogService {

    private final AuditLogPersistencePort persistencePort;
    private final Clock clock;

    public AuditLogService(AuditLogPersistencePort persistencePort, Clock clock) {
        this.persistencePort = persistencePort;
        this.clock = clock;
    }

    /**
     * Persiste un evento de auditoría en una transacción independiente para que el registro
     * sobreviva incluso si la operación de negocio termina en error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        LocalDateTime occurredAt = auditLog.occurredAt() == null ? LocalDateTime.now(clock) : auditLog.occurredAt();
        persistencePort.save(auditLog.withOccurredAt(occurredAt));
    }

    @Transactional(readOnly = true)
    public AuditLogPage search(AuditLogSearchCriteria criteria) {
        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), 200);
        LocalDateTime from = criteria.from();
        if (criteria.lastHours() != null && criteria.lastHours() > 0) {
            LocalDateTime lastWindow = LocalDateTime.now(clock).minusHours(criteria.lastHours());
            if (from == null || lastWindow.isAfter(from)) {
                from = lastWindow;
            }
        }
        AuditLogSearchCriteria normalized = new AuditLogSearchCriteria(
                normalize(criteria.userEmail(), 254),
                normalizeUpper(criteria.entityType(), 40),
                normalize(criteria.action(), 80),
                normalizeUpper(criteria.result(), 20),
                from,
                criteria.to(),
                criteria.lastHours(),
                page,
                size);
        return persistencePort.search(normalized);
    }

    private String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizeUpper(String value, int maxLength) {
        String normalized = normalize(value, maxLength);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
