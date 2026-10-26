package com.syncevent.attendance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendancePersistencePort {

    Optional<AttendanceRegistration> findByQrTokenForUpdate(String qrToken);

    Optional<AttendanceRegistration> findActiveByEventAndParticipantForUpdate(
            UUID eventId, UUID participantId);

    List<AttendanceCandidate> searchActiveRegistrations(UUID eventId, String query, int limit);

    Optional<CheckIn> findByRegistrationId(UUID registrationId);

    CheckIn save(CheckIn checkIn);
}
