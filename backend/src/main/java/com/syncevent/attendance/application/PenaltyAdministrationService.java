package com.syncevent.attendance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.attendance.domain.FineAlreadyResolvedException;
import com.syncevent.attendance.domain.FineNotFoundException;
import com.syncevent.attendance.domain.FinePage;
import com.syncevent.attendance.domain.FinePolicy;
import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.attendance.domain.ParticipantFine;
import com.syncevent.attendance.domain.PenaltyPersistencePort;
import com.syncevent.users.application.AuthActivityRecorder;

@Service
public class PenaltyAdministrationService {

    private final PenaltyPersistencePort persistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final Clock clock;

    public PenaltyAdministrationService(
            PenaltyPersistencePort persistencePort,
            AuthActivityRecorder activityRecorder,
            Clock clock) {
        this.persistencePort = persistencePort;
        this.activityRecorder = activityRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FinePolicy getPolicy() {
        return persistencePort.findPolicy();
    }

    @Transactional
    public FinePolicy updatePolicy(int absenceThreshold, int windowDays, UUID administratorId, String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        FinePolicy policy = persistencePort.savePolicy(
                new FinePolicy(absenceThreshold, windowDays, administratorId, now));
        activityRecorder.record(administratorId, email, "ATTENDANCE_POLICY_UPDATED", now);
        return policy;
    }

    @Transactional(readOnly = true)
    public FinePage searchFines(FineStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("La página debe ser positiva y el tamaño debe estar entre 1 y 100.");
        }
        return persistencePort.searchFines(status, page, size);
    }

    @Transactional
    public ParticipantFine resolveFine(UUID fineId, UUID administratorId, String email) {
        ParticipantFine fine = persistencePort.findFineByIdForUpdate(fineId)
                .orElseThrow(() -> new FineNotFoundException(fineId));
        ParticipantFine resolved = fine.resolve(administratorId, LocalDateTime.now(clock));
        ParticipantFine saved = persistencePort.saveFine(resolved);
        activityRecorder.record(
                administratorId, email, "FINE_RESOLVED", saved.resolvedAt());
        return saved;
    }
}
