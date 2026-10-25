package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.events.domain.port.EventSearchCriteria;
import com.syncevent.events.domain.port.EventSearchPage;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.events.domain.port.SortDirection;

@Component
public class EventPersistenceAdapter implements EventPersistencePort {

    private final SpringDataEventRepository repository;

    public EventPersistenceAdapter(SpringDataEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasScheduleConflict(LocalDateTime startAt, LocalDateTime endAt, String location) {
        return repository.existsByLocationAndStartAtLessThanAndEndAtGreaterThanAndStatusNotAndStatusNot(
                location,
                endAt,
                startAt,
                EstadoEvento.FINALIZADO,
                EstadoEvento.CANCELADO);
    }

    @Override
    public Event save(Event event) {
        return repository.save(EventEntity.fromDomain(event)).toDomain();
    }

    @Override
    public Optional<Event> findById(UUID id) {
        return repository.findById(id).map(EventEntity::toDomain);
    }

    @Override
    public EventSearchPage search(EventSearchCriteria criteria) {
        Specification<EventEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.type() != null) {
                predicates.add(builder.equal(root.get("type"), criteria.type()));
            }
            if (criteria.modality() != null) {
                predicates.add(builder.equal(root.get("modality"), criteria.modality()));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.visibleStatuses() != null) {
                predicates.add(root.get("status").in(criteria.visibleStatuses()));
            }
            if (criteria.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("startAt"), criteria.from().atStartOfDay()));
            }
            if (criteria.to() != null) {
                predicates.add(builder.lessThan(
                        root.get("startAt"), criteria.to().plusDays(1).atStartOfDay()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        String sortProperty = switch (criteria.sort()) {
            case DATE -> "startAt";
            case NAME -> "name";
        };
        Sort.Direction direction = criteria.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Page<EventEntity> result = repository.findAll(
                specification,
                PageRequest.of(
                        criteria.page(),
                        criteria.size(),
                        Sort.by(direction, sortProperty).and(Sort.by(Sort.Direction.ASC, "id"))));

        return new EventSearchPage(
                result.getContent().stream().map(EventEntity::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize());
    }
}