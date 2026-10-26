package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAttendanceAbsenceRepository
        extends JpaRepository<AttendanceAbsenceEntity, UUID> {

    boolean existsByRegistrationId(UUID registrationId);

    long countByParticipantIdAndRecordedAtGreaterThanEqual(UUID participantId, LocalDateTime since);
}
