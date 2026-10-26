package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.FinePolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "attendance_policy")
public class AttendancePolicyEntity {

    @Id
    private Integer id;

    @Column(name = "absence_threshold", nullable = false)
    private int absenceThreshold;

    @Column(name = "window_days", nullable = false)
    private int windowDays;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AttendancePolicyEntity() {
    }

    FinePolicy toDomain() {
        return new FinePolicy(absenceThreshold, windowDays, updatedBy, updatedAt);
    }

    void update(FinePolicy policy) {
        absenceThreshold = policy.absenceThreshold();
        windowDays = policy.windowDays();
        updatedBy = policy.updatedBy();
        updatedAt = policy.updatedAt();
    }
}
