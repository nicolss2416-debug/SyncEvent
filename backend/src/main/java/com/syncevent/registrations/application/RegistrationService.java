package com.syncevent.registrations.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.attendance.domain.AttendancePersistencePort;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.application.ParticipantNotFoundException;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationCancellationNotAllowedException;
import com.syncevent.registrations.domain.RegistrationNotFoundException;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.registrations.domain.PendingFineCheckPort;
import com.syncevent.users.application.UserAccountNotFoundException;
import com.syncevent.users.application.AuthActivityRecorder;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@Service
public class RegistrationService {

    private final EventPersistencePort eventPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final RegistrationPersistencePort registrationPersistencePort;
    private final AttendancePersistencePort attendancePersistencePort;
    private final PendingFineCheckPort pendingFineCheckPort;
    private final AuthActivityRecorder activityRecorder;
    private final UserPersistencePort userPersistencePort;
    private final SecureQrTokenGenerator qrTokenGenerator;
    private final Clock clock;

    public RegistrationService(
            EventPersistencePort eventPersistencePort,
            ParticipantPersistencePort participantPersistencePort,
            RegistrationPersistencePort registrationPersistencePort,
            AttendancePersistencePort attendancePersistencePort,
            PendingFineCheckPort pendingFineCheckPort,
            AuthActivityRecorder activityRecorder,
            UserPersistencePort userPersistencePort,
            SecureQrTokenGenerator qrTokenGenerator,
            Clock clock) {
        this.eventPersistencePort = eventPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.registrationPersistencePort = registrationPersistencePort;
        this.attendancePersistencePort = attendancePersistencePort;
        this.pendingFineCheckPort = pendingFineCheckPort;
        this.activityRecorder = activityRecorder;
        this.userPersistencePort = userPersistencePort;
        this.qrTokenGenerator = qrTokenGenerator;
        this.clock = clock;
    }

    @Transactional
    public Registration registerForGuest(UUID eventId, UUID guestAccountId, String actorEmail) {
        var guest = userPersistencePort.findById(guestAccountId)
                .orElseThrow(GuestParticipantNotLinkedException::new);
        if (!guest.getRoles().contains(UserRole.INVITADO)) {
            throw new RegistrationNotAllowedException("Esta operación solo está disponible para cuentas Invitado.");
        }
        if (guest.getParticipantId() == null) {
            throw new GuestParticipantNotLinkedException();
        }
        return register(eventId, guest.getParticipantId(), guestAccountId, actorEmail);
    }

    @Transactional
    public Registration register(UUID eventId, UUID participantId, UUID createdBy, String actorEmail) {
        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        participantPersistencePort.findByIdForUpdate(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
        if (pendingFineCheckPort.hasPendingFine(participantId)) {
            throw new PendingFineRegistrationException();
        }
        if (event.getStatus() != EstadoEvento.PUBLICADO) {
            throw new RegistrationNotAllowedException(
                    "El evento debe estar PUBLICADO para aceptar inscripciones; su estado actual es "
                            + event.getStatus() + ".");
        }
        if (registrationPersistencePort.existsActiveByEventIdAndParticipantId(eventId, participantId)) {
            throw new DuplicateRegistrationException();
        }

        long activeRegistrations = registrationPersistencePort.countActiveByEventId(eventId);
        if (activeRegistrations >= event.getCapacity()) {
            throw new RegistrationNotAllowedException("El evento alcanzó su aforo máximo.");
        }

        registrationPersistencePort.findScheduleConflict(participantId, eventId, event)
                .ifPresent(conflictingEvent -> {
                    throw new RegistrationScheduleConflictException(
                            conflictingEvent.getId(), conflictingEvent.getName());
                });

        LocalDateTime now = LocalDateTime.now(clock);
        Registration registration = registrationPersistencePort.save(Registration.create(
                eventId,
                participantId,
                qrTokenGenerator.generate(),
                createdBy,
                now));
        activityRecorder.record(createdBy, actorEmail, "REGISTRATION_CREATED", now);
        return registration;
    }

    @Transactional
    public RegistrationCancellation cancel(
            UUID eventId, UUID registrationId, UUID canceledBy, String actorEmail, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 2000) {
            throw new IllegalArgumentException("El motivo es obligatorio y no puede superar 2000 caracteres.");
        }

        Registration initial = registrationPersistencePort.findById(registrationId)
                .filter(registration -> registration.getEventId().equals(eventId))
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));
        Event event = eventPersistencePort.findByIdForUpdate(initial.getEventId())
                .orElseThrow(() -> new EventNotFoundException(eventId));
        Registration registration = registrationPersistencePort.findByIdForUpdate(registrationId)
                .filter(current -> current.getEventId().equals(eventId))
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));

        var actor = userPersistencePort.findById(canceledBy)
                .orElseThrow(() -> new UserAccountNotFoundException(canceledBy));
        boolean privileged = actor.getRoles().contains(UserRole.ADMINISTRADOR)
                || actor.getRoles().contains(UserRole.OPERADOR);
        if (!privileged && (!actor.getRoles().contains(UserRole.INVITADO)
                || !registration.getParticipantId().equals(actor.getParticipantId()))) {
            throw new RegistrationOwnershipException();
        }
        if (event.getStatus() == EstadoEvento.FINALIZADO || event.getStatus() == EstadoEvento.CANCELADO) {
            throw new RegistrationCancellationNotAllowedException(
                    "No se pueden cancelar inscripciones de eventos finalizados o cancelados.");
        }
        if (attendancePersistencePort.findByRegistrationId(registrationId).isPresent()) {
            throw new RegistrationCancellationNotAllowedException(
                    "No se puede cancelar una inscripción que ya tiene asistencia registrada.");
        }

        Registration canceled = registration.cancel();
        LocalDateTime canceledAt = LocalDateTime.now(clock);
        registrationPersistencePort.saveCancellation(canceled, canceledBy, normalizedReason, canceledAt);
        activityRecorder.record(canceledBy, actorEmail, "REGISTRATION_CANCELLED", canceledAt);
        return new RegistrationCancellation(
                canceled.getId(),
                canceled.getEventId(),
                canceled.getParticipantId(),
                canceledBy,
                normalizedReason,
                canceledAt);
    }
}
