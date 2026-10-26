package com.syncevent.attendance.api;

import jakarta.validation.constraints.Min;

public record UpdateAttendancePolicyRequest(
        @Min(1) int absenceThreshold,
        @Min(1) int windowDays) {
}
