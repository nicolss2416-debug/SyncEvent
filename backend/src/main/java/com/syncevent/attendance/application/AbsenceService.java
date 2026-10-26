package com.syncevent.attendance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.attendance.domain.AttendanceAbsence;
import com.syncevent.attendance.domain.FinePolicy;
import com.syncevent.attendance.domain.ParticipantFine;
import com.syncevent.attendance.domain.PenaltyPersistencePort;
import com.syncevent.events.domain.port.EventFinalizationPort;
import com.syncevent.participants.domain.ParticipantPersistencePort;

@Service
public class AbsenceService implements EventFinalizationPort {

    private final PenaltyPersistencePort persistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final Clock clock;

    public AbsenceService(
            PenaltyPersistencePort persistencePort,
            ParticipantPersistencePort participantPersistencePort,
            Clock clock) {
        this.persistencePort = persistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void processFinalizedEvent(UUID eventId) {
        var unattendedRegistrations = persistencePort.findActiveRegistrationsWithoutCheckIn(eventId);
        if (unattendedRegistrations.isEmpty()) {
            return;
        }
        FinePolicy policy = persistencePort.findPolicyForUpdate();
        LocalDateTime recordedAt = LocalDateTime.now(clock);

        for (var registration : unattendedRegistrations) {
            participantPersistencePort.findByIdForUpdate(registration.participantId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe el participante de la inscripción " + registration.registrationId()));
            if (persistencePort.existsAbsenceByRegistrationId(registration.registrationId())) {
                continue;
            }

            AttendanceAbsence absence = persistencePort.saveAbsence(new AttendanceAbsence(
                    UUID.randomUUID(),
                    registration.registrationId(),
                    eventId,
                    registration.participantId(),
                    recordedAt));
            LocalDateTime windowStart = recordedAt.minusDays(policy.windowDays());
            long absenceCount = persistencePort.countAbsencesSince(registration.participantId(), windowStart);
            if (absenceCount > policy.absenceThreshold()
                    && !persistencePort.existsPendingFine(registration.participantId())) {
                persistencePort.saveFine(new ParticipantFine(
                        UUID.randomUUID(),
                        registration.participantId(),
                        absence.id(),
                        policy.absenceThreshold(),
                        policy.windowDays(),
                        com.syncevent.attendance.domain.FineStatus.PENDIENTE,
                        recordedAt,
                        null,
                        null));
            }
        }
    }
}
