package com.syncevent.registrations.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Event;
import com.syncevent.events.application.EventNotFoundException;
import com.syncevent.events.domain.port.EventPersistencePort;
import com.syncevent.participants.application.ParticipantNotFoundException;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import com.syncevent.registrations.domain.Registration;
import com.syncevent.registrations.domain.RegistrationPersistencePort;
import com.syncevent.users.application.AuthActivityRecorder;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;

@Service
public class RegistrationService {

    private final EventPersistencePort eventPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final RegistrationPersistencePort registrationPersistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final UserPersistencePort userPersistencePort;
    private final SecureQrTokenGenerator qrTokenGenerator;
    private final Clock clock;

    public RegistrationService(
            EventPersistencePort eventPersistencePort,
            ParticipantPersistencePort participantPersistencePort,
            RegistrationPersistencePort registrationPersistencePort,
            AuthActivityRecorder activityRecorder,
            UserPersistencePort userPersistencePort,
            SecureQrTokenGenerator qrTokenGenerator,
            Clock clock) {
        this.eventPersistencePort = eventPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.registrationPersistencePort = registrationPersistencePort;
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
        participantPersistencePort.findByIdForUpdate(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        Event event = eventPersistencePort.findByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
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
}
