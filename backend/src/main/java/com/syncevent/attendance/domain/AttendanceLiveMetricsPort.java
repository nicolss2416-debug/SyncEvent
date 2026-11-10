package com.syncevent.attendance.domain;

import java.util.UUID;

public interface AttendanceLiveMetricsPort {

    long countCheckIns(UUID eventId);
}
