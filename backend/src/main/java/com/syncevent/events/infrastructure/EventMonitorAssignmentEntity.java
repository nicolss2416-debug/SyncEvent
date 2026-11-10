package com.syncevent.events.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.events.domain.MonitorAssignment;
import com.syncevent.users.infrastructure.UserAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_monitor_assignments")
@IdClass(EventMonitorAssignmentId.class)
public class EventMonitorAssignmentEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, insertable = false, updatable = false)
    private EventEntity event;

    @Id
    @Column(name = "monitor_id", nullable = false, updatable = false)
    private UUID monitorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false, insertable = false, updatable = false)
    private UserAccountEntity monitor;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private UUID assignedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false, insertable = false, updatable = false)
    private UserAccountEntity assignedByUser;

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
