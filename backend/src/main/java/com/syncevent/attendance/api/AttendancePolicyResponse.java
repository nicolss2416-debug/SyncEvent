package com.syncevent.attendance.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.FinePolicy;

public record AttendancePolicyResponse(
        int absenceThreshold,
        int windowDays,
        UUID updatedBy,
        LocalDateTime updatedAt) {

    static AttendancePolicyResponse from(FinePolicy policy) {
        return new AttendancePolicyResponse(
                policy.absenceThreshold(), policy.windowDays(), policy.updatedBy(), policy.updatedAt());
    }
}
