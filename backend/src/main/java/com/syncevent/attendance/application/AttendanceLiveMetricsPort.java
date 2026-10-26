package com.syncevent.attendance.application;

import java.util.UUID;

public interface AttendanceLiveMetricsPort {

    long countCheckIns(UUID eventId);
}
