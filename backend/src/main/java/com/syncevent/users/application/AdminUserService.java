package com.syncevent.users.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserAccountSearchPage;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.users.domain.UserRole;
import com.syncevent.participants.application.ParticipantNotFoundException;
import com.syncevent.participants.domain.ParticipantPersistencePort;

@Service
public class AdminUserService {

    private final UserPersistencePort userPersistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ParticipantPersistencePort participantPersistencePort;

    public AdminUserService(
            UserPersistencePort userPersistencePort,
            AuthActivityRecorder activityRecorder,
            PasswordEncoder passwordEncoder,
            Clock clock,
            ParticipantPersistencePort participantPersistencePort) {
        this.userPersistencePort = userPersistencePort;
        this.activityRecorder = activityRecorder;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.participantPersistencePort = participantPersistencePort;
    }

    @Transactional
    public UserAccount create(String email, String rawPassword, Set<UserRole> roles, UUID actorId) {
        String normalizedEmail = normalizeEmail(email);
        ensureEmailAvailable(normalizedEmail);
        LocalDateTime now = LocalDateTime.now(clock);
        UserAccount account = UserAccount.createManaged(
                normalizedEmail, passwordEncoder.encode(rawPassword), roles, now);
        account = saveEmailAware(account);
        activityRecorder.record(actorId, account.getEmail(), "ADMIN_USER_CREATED", now);
        return account;
    }

    @Transactional(readOnly = true)
    public UserAccountSearchPage findAll(int page, int size) {
        return userPersistencePort.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public UserAccount findById(UUID id) {
        return userPersistencePort.findById(id).orElseThrow(() -> new UserAccountNotFoundException(id));
    }

    @Transactional
    public UserAccount updateEmail(UUID id, String email, UUID actorId) {
        UserAccount account = findById(id);
        String normalizedEmail = normalizeEmail(email);
        userPersistencePort.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateUserEmailException();
                });

        UserAccount updated = account.updateEmail(normalizedEmail);
        if (account.getEmail().equals(updated.getEmail())) {
            return account;
        }
        updated = saveEmailAware(updated);
        activityRecorder.record(actorId, updated.getEmail(), "ADMIN_USER_UPDATED", LocalDateTime.now(clock));
        return updated;
    }

    @Transactional
    public UserAccount assignRole(UUID id, UserRole role, UUID actorId) {
        UserAccount account = findById(id);
        if (account.getRoles().contains(role)) {
            return account;
        }

        UserAccount updated = userPersistencePort.save(account.assignRole(role));
        activityRecorder.record(actorId, updated.getEmail(), "USER_ROLE_ADDED", LocalDateTime.now(clock));
        return updated;
    }

    @Transactional
    public UserAccount linkParticipant(UUID userId, UUID participantId, UUID actorId, String actorEmail) {
        UserAccount account = userPersistencePort.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
        if (!account.getRoles().contains(UserRole.INVITADO)) {
            throw new ParticipantLinkNotAllowedException(
                    "Solo las cuentas Invitado pueden vincularse a participantes.");
        }
        if (participantId.equals(account.getParticipantId())) {
            return account;
        }
        if (account.getParticipantId() != null || userPersistencePort.existsByParticipantId(participantId)) {
            throw new ParticipantAlreadyLinkedException(
                    "La cuenta o el participante ya tiene otra vinculación.");
        }
        participantPersistencePort.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        UserAccount linked;
        try {
            linked = userPersistencePort.save(account.linkParticipant(participantId));
        } catch (DataIntegrityViolationException exception) {
            if (isParticipantAlreadyLinked(exception)) {
                throw new ParticipantAlreadyLinkedException(
                        "La cuenta o el participante ya tiene otra vinculación.");
            }
            throw exception;
        }
        activityRecorder.record(actorId, actorEmail, "PARTICIPANT_LINKED", LocalDateTime.now(clock));
        return linked;
    }

    @Transactional
    public UserAccount setActive(UUID id, boolean active, UUID actorId) {
        UserAccount account = findById(id);
        if (account.isActive() == active) {
            return account;
        }
        if (!active
                && account.getRoles().contains(UserRole.ADMINISTRADOR)
                && userPersistencePort.countActiveAdministrators() <= 1) {
            throw new LastAdministratorException();
        }

        UserAccount updated = userPersistencePort.save(account.withActive(active));
        activityRecorder.record(
                actorId,
                updated.getEmail(),
                active ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                LocalDateTime.now(clock));
        return updated;
    }

    @Transactional
    public void provisionBootstrapAdministrator(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount existing = userPersistencePort.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            if (!existing.isActive() || !existing.getRoles().contains(UserRole.ADMINISTRADOR)) {
                throw new IllegalStateException(
                        "La cuenta configurada para bootstrap ya existe y no es un Administrador activo.");
            }
            return;
        }

        if (rawPassword.length() < 8 || rawPassword.length() > 128) {
            throw new IllegalStateException(
                    "La contraseña de bootstrap debe tener entre 8 y 128 caracteres.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        UserAccount account = UserAccount.createManaged(
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                Set.of(UserRole.ADMINISTRADOR),
                now);
        account = saveEmailAware(account);
        activityRecorder.record(account.getId(), normalizedEmail, "ADMIN_BOOTSTRAP", now);
    }

    private void ensureEmailAvailable(String email) {
        if (userPersistencePort.existsByEmail(email)) {
            throw new DuplicateUserEmailException();
        }
    }

    private UserAccount saveEmailAware(UserAccount account) {
        try {
            return userPersistencePort.save(account);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new DuplicateUserEmailException();
            }
            throw exception;
        }
    }

    private boolean isDuplicateEmail(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_user_accounts_email".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isParticipantAlreadyLinked(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_user_accounts_participant".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
