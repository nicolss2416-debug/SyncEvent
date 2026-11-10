package com.syncevent.audit.infrastructure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.audit.domain.AuditLog;
import com.syncevent.audit.domain.AuditLogPage;
import com.syncevent.audit.domain.AuditLogPersistencePort;
import com.syncevent.audit.domain.AuditLogSearchCriteria;

import jakarta.persistence.criteria.Predicate;

@Repository
public class AuditLogPersistenceAdapter implements AuditLogPersistencePort {

    private final SpringDataAuditLogRepository repository;

    public AuditLogPersistenceAdapter(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        return repository.save(AuditLogEntity.fromDomain(auditLog)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPage search(AuditLogSearchCriteria criteria) {
        Specification<AuditLogEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.userEmail() != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("userEmail")),
                        "%" + criteria.userEmail().toLowerCase() + "%"));
            }
            if (criteria.entityType() != null) {
                predicates.add(builder.equal(root.get("entityType"), criteria.entityType()));
            }
            if (criteria.action() != null) {
                predicates.add(builder.like(
                        builder.lower(root.get("action")),
                        "%" + criteria.action().toLowerCase() + "%"));
            }
            if (criteria.result() != null) {
                predicates.add(builder.equal(root.get("result"), criteria.result()));
            }
            if (criteria.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), criteria.to()));
            }
            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<AuditLogEntity> result = repository.findAll(
                specification,
                PageRequest.of(criteria.page(), criteria.size(), Sort.by(Sort.Direction.DESC, "id")));

        List<AuditLog> items = result.getContent().stream()
                .map(AuditLogEntity::toDomain)
                .toList();
        return new AuditLogPage(items, result.getTotalElements(), criteria.page(), criteria.size());
    }
}
