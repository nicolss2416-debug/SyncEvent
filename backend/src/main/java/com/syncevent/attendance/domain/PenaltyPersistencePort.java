package com.syncevent.attendance.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenaltyPersistencePort {

    List<UnattendedRegistration> findActiveRegistrationsWithoutCheckIn(UUID eventId);

    boolean existsAbsenceByRegistrationId(UUID registrationId);

    AttendanceAbsence saveAbsence(AttendanceAbsence absence);

    long countAbsencesSince(UUID participantId, LocalDateTime since);

    FinePolicy findPolicy();

    FinePolicy findPolicyForUpdate();

    FinePolicy savePolicy(FinePolicy policy);

    boolean existsPendingFine(UUID participantId);

    Optional<ParticipantFine> findFineByIdForUpdate(UUID fineId);

    FinePage searchFines(FineStatus status, int page, int size);

    ParticipantFine saveFine(ParticipantFine fine);
}
