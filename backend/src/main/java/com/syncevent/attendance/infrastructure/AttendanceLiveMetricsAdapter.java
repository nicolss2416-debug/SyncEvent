package com.syncevent.attendance.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.attendance.application.AttendanceLiveMetricsPort;

@Repository
public class AttendanceLiveMetricsAdapter implements AttendanceLiveMetricsPort {

    private final SpringDataCheckInRepository repository;

    public AttendanceLiveMetricsAdapter(SpringDataCheckInRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countCheckIns(UUID eventId) {
        return repository.countByEventId(eventId);
    }
}
