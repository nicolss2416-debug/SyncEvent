package com.syncevent.attendance.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCheckInRepository extends JpaRepository<CheckInEntity, UUID> {

    @Query("""
            SELECT COUNT(checkIn)
            FROM CheckInEntity checkIn
            JOIN RegistrationEntity registration ON registration.id = checkIn.registrationId
            WHERE registration.event.id = :eventId
            """)
    long countByEventId(@Param("eventId") UUID eventId);
}
