package com.syncevent.events.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.events.domain.model.MonitorAssignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_monitor_assignments")
@IdClass(EventMonitorAssignmentId.class)
public class EventMonitorAssignmentEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Id
    @Column(name = "monitor_id", nullable = false, updatable = false)
    private UUID monitorId;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    protected EventMonitorAssignmentEntity() {
    }

    static EventMonitorAssignmentEntity fromDomain(MonitorAssignment assignment) {
        EventMonitorAssignmentEntity entity = new EventMonitorAssignmentEntity();
        entity.eventId = assignment.eventId();
        entity.monitorId = assignment.monitorId();
        entity.assignedBy = assignment.assignedBy();
        entity.assignedAt = assignment.assignedAt();
        return entity;
    }

    MonitorAssignment toDomain() {
        return new MonitorAssignment(eventId, monitorId, assignedBy, assignedAt);
    }
}
