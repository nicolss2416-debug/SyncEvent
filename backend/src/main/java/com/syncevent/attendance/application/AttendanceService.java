package com.syncevent.attendance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.attendance.domain.AttendanceCandidate;
import com.syncevent.attendance.domain.AttendancePersistencePort;
import com.syncevent.attendance.domain.AttendanceRegistration;
import com.syncevent.attendance.domain.CheckIn;
import com.syncevent.attendance.domain.CheckInMethod;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.registrations.domain.RegistrationStatus;
import com.syncevent.users.application.AuthActivityRecorder;

@Service
public class AttendanceService {

    private static final int MAX_SEARCH_RESULTS = 50;

    private final EventPersistencePort eventPersistencePort;
    private final AttendancePersistencePort attendancePersistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final Clock clock;

    public AttendanceService(
            EventPersistencePort eventPersistencePort,
            AttendancePersistencePort attendancePersistencePort,
            AuthActivityRecorder activityRecorder,
            Clock clock) {
        this.eventPersistencePort = eventPersistencePort;
        this.attendancePersistencePort = attendancePersistencePort;
        this.activityRecorder = activityRecorder;
        this.clock = clock;
    }

    @Transactional
    public AttendanceCheckIn checkInByQr(
            UUID eventId, String qrToken, UUID checkedInBy, String actorEmail) {
        ensureEventInProgress(eventId, true);
        AttendanceRegistration registration = attendancePersistencePort.findByQrTokenForUpdate(qrToken)
                .filter(candidate -> candidate.eventId().equals(eventId))
                .filter(candidate -> candidate.registrationStatus() == RegistrationStatus.ACTIVA)
                .orElseThrow(InvalidQrCodeException::new);
        return recordCheckIn(registration, CheckInMethod.QR, checkedInBy, actorEmail);
    }

    @Transactional
    public AttendanceCheckIn checkInManually(
            UUID eventId, UUID participantId, UUID checkedInBy, String actorEmail) {
        ensureEventInProgress(eventId, true);
        AttendanceRegistration registration =
                attendancePersistencePort.findActiveByEventAndParticipantForUpdate(eventId, participantId)
                        .orElseThrow(ActiveRegistrationNotFoundException::new);
        return recordCheckIn(registration, CheckInMethod.MANUAL, checkedInBy, actorEmail);
    }

    @Transactional(readOnly = true)
    public List<AttendanceCandidate> searchRegistrations(UUID eventId, String query) {
        ensureEventInProgress(eventId, false);
        return attendancePersistencePort.searchActiveRegistrations(
                eventId, query.trim(), MAX_SEARCH_RESULTS);
    }

    private AttendanceCheckIn recordCheckIn(
            AttendanceRegistration registration,
            CheckInMethod method,
            UUID checkedInBy,
            String actorEmail) {
        attendancePersistencePort.findByRegistrationId(registration.registrationId())
                .ifPresent(existing -> {
                    throw new CheckInAlreadyRecordedException(existing.checkedInAt());
                });
        LocalDateTime checkedInAt = LocalDateTime.now(clock);
        CheckIn checkIn = attendancePersistencePort.save(
                new CheckIn(registration.registrationId(), checkedInAt, checkedInBy, method));
        activityRecorder.record(
                checkedInBy,
                actorEmail,
                method == CheckInMethod.MANUAL ? "ATTENDANCE_MANUAL" : "ATTENDANCE_QR",
                checkedInAt);
        return new AttendanceCheckIn(registration, checkIn);
    }

    private void ensureEventInProgress(UUID eventId, boolean lock) {
        var event = (lock
                        ? eventPersistencePort.findByIdForUpdate(eventId)
                        : eventPersistencePort.findById(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (event.getStatus() != EstadoEvento.EN_CURSO) {
            throw new CheckInNotAllowedException(
                    "El evento debe estar EN_CURSO para registrar asistencia.");
        }
    }
}
