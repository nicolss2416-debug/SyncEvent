package com.syncevent.attendance.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCheckInRepository extends JpaRepository<CheckInEntity, UUID> {
}
