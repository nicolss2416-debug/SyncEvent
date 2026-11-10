package com.syncevent.users.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.AuthSessionPersistencePort;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;
import com.syncevent.participants.application.DuplicateParticipantException;
import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class AuthService {

    private static final String DUMMY_PASSWORD = "not-a-real-account-password";

    private final UserPersistencePort userPersistencePort;
    private final ParticipantPersistencePort participantPersistencePort;
    private final AuthSessionPersistencePort sessionPersistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserPersistencePort userPersistencePort,
            ParticipantPersistencePort participantPersistencePort,
            AuthSessionPersistencePort sessionPersistencePort,
            AuthActivityRecorder activityRecorder,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            Clock clock) {
        this.userPersistencePort = userPersistencePort;
        this.participantPersistencePort = participantPersistencePort;
        this.sessionPersistencePort = sessionPersistencePort;
        this.activityRecorder = activityRecorder;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public UserAccount register(String email, String rawPassword) {
        return register(email, rawPassword, null);
    }

    @Transactional
    public UserAccount register(String email, String rawPassword, GuestProfileCommand profile) {
        String normalizedEmail = normalizeEmail(email);
        if (userPersistencePort.existsByEmail(normalizedEmail)) {
            throw new DuplicateUserEmailException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        UserAccount registered;
        try {
            registered = userPersistencePort.save(UserAccount.register(
                    normalizedEmail,
                    passwordEncoder.encode(rawPassword),
                    now));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new DuplicateUserEmailException();
            }
            throw exception;
        }
        if (profile != null) {
            completeProfile(registered, normalizedEmail, profile);
            registered = userPersistencePort.findById(registered.getId()).orElseThrow();
        }
        activityRecorder.record(registered.getId(), normalizedEmail, "REGISTER", now);
        return registered;
    }

    @Transactional
    public Participant completeGuestProfile(UUID userId, String email, GuestProfileCommand profile) {
        UserAccount account = userPersistencePort.findByIdForUpdate(userId)
                .orElseThrow(() -> new AuthenticationFailedException());
        if (!account.getRoles().contains(com.syncevent.users.domain.UserRole.INVITADO)) {
            throw new IllegalArgumentException("Solo las cuentas Invitado pueden crear este perfil.");
        }
        if (account.getParticipantId() != null) {
            return participantPersistencePort.findById(account.getParticipantId()).orElseThrow();
        }
        Participant participant = createParticipant(account, profile);
        userPersistencePort.save(account.linkParticipant(participant.getId()));
        activityRecorder.record(userId, email, "PARTICIPANT_PROFILE_COMPLETED", LocalDateTime.now(clock));
        return participant;
    }

    private void completeProfile(UserAccount account, String email, GuestProfileCommand profile) {
        Participant participant = createParticipant(account, profile);
        userPersistencePort.save(account.linkParticipant(participant.getId()));
    }

    private Participant createParticipant(UserAccount account, GuestProfileCommand profile) {
        if (profile == null || profile.documentType() == null || profile.documentType().isBlank()
                || profile.documentNumber() == null || profile.documentNumber().isBlank()
                || profile.firstName() == null || profile.firstName().isBlank()
                || profile.lastName() == null || profile.lastName().isBlank()
                || profile.phone() == null || profile.phone().isBlank()) {
            throw new IllegalArgumentException("Completa tus datos de participante.");
        }
        String documentType = profile.documentType().trim().toUpperCase(Locale.ROOT);
        String documentNumber = profile.documentNumber().trim().toUpperCase(Locale.ROOT);
        var existingOpt = participantPersistencePort.findByDocument(documentType, documentNumber);
        if (existingOpt.isPresent()) {
            Participant existing = existingOpt.get();
            if (existing.getEmail().equalsIgnoreCase(account.getEmail())
                    && !userPersistencePort.existsByParticipantId(existing.getId())) {
                return existing;
            }
            throw new DuplicateParticipantException(existing.getId());
        }
        try {
            return participantPersistencePort.save(Participant.register(
                    documentType,
                    documentNumber,
                    profile.firstName(),
                    profile.lastName(),
                    account.getEmail(),
                    profile.phone(),
                    account.getEmail()));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateParticipantException(null);
        }
    }

    @Transactional
    public IssuedAccessToken login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount account = userPersistencePort.findByEmail(normalizedEmail).orElse(null);
        String passwordHash = account == null ? dummyPasswordHash : account.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(rawPassword, passwordHash);

        if (account == null || !account.isActive() || !passwordMatches) {
            activityRecorder.recordFailedLogin(normalizedEmail, LocalDateTime.now(clock));
            throw new AuthenticationFailedException();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (account.getRoles().isEmpty()) {
            account = userPersistencePort.save(account.assignRole(com.syncevent.users.domain.UserRole.INVITADO));
        }
        UUID sessionId = UUID.randomUUID();
        sessionPersistencePort.create(sessionId, account.getId(), now);
        List<String> roles = account.getRoles().stream().map(Enum::name).sorted().toList();
        IssuedAccessToken issued = tokenService.issue(account.getId(), account.getEmail(), sessionId, roles);
        activityRecorder.record(account.getId(), account.getEmail(), "LOGIN_SUCCESS", now);
        return issued;
    }

    @Transactional
    public IssuedAccessToken refresh(Jwt currentToken) {
        UUID userId = UUID.fromString(currentToken.getClaimAsString("uid"));
        UUID sessionId = UUID.fromString(currentToken.getClaimAsString("sid"));
        UserAccount account = userPersistencePort.findById(userId).orElse(null);
        if (account != null) {
            if (!account.isActive()) {
                throw new AuthenticationFailedException();
            }
            userPersistencePort.save(account);
            List<String> liveRoles = account.getRoles().stream().map(Enum::name).sorted().toList();
            return tokenService.issue(account.getId(), account.getEmail(), sessionId, liveRoles);
        }
        List<String> roles = currentToken.getClaimAsStringList("roles");
        return tokenService.issue(userId, currentToken.getSubject(), sessionId, roles);
    }

    @Transactional
    public void logout(Jwt currentToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID userId = UUID.fromString(currentToken.getClaimAsString("uid"));
        UUID sessionId = UUID.fromString(currentToken.getClaimAsString("sid"));
        sessionPersistencePort.revoke(sessionId, now);
        activityRecorder.record(userId, currentToken.getSubject(), "LOGOUT", now);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<UserAccount> findById(UUID userId) {
        return userPersistencePort.findById(userId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<UserAccount> findByEmail(String email) {
        return userPersistencePort.findByEmail(email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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
}
