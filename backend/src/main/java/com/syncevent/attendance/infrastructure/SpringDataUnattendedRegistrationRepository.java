package com.syncevent.attendance.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.syncevent.attendance.domain.UnattendedRegistration;
import com.syncevent.registrations.domain.RegistrationStatus;

interface SpringDataUnattendedRegistrationRepository
        extends Repository<com.syncevent.registrations.infrastructure.RegistrationEntity, UUID> {

    @Query("""
            SELECT new com.syncevent.attendance.domain.UnattendedRegistration(r.id, r.participant.id)
            FROM RegistrationEntity r
            WHERE r.event.id = :eventId
              AND r.status = :activeStatus
              AND NOT EXISTS (
                  SELECT a.id FROM AttendanceAbsenceEntity a WHERE a.registrationId = r.id
              )
              AND NOT EXISTS (
                  SELECT c.registrationId FROM CheckInEntity c WHERE c.registrationId = r.id
              )
            """)
    List<UnattendedRegistration> findActiveWithoutCheckIn(
            @Param("eventId") UUID eventId,
            @Param("activeStatus") RegistrationStatus activeStatus);
}
