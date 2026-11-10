package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAttendanceAbsenceRepository
        extends JpaRepository<AttendanceAbsenceEntity, UUID> {

    boolean existsByRegistrationId(UUID registrationId);

    @Query("""
            SELECT COUNT(a) FROM AttendanceAbsenceEntity a
            WHERE a.registration.participant.id = :participantId
              AND a.recordedAt >= :since
            """)
    long countByParticipantIdAndRecordedAtGreaterThanEqual(
            @Param("participantId") UUID participantId,
            @Param("since") LocalDateTime since);
}
