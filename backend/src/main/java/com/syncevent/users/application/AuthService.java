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
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class AuthService {

    private static final String DUMMY_PASSWORD = "not-a-real-account-password";

    private final UserPersistencePort userPersistencePort;
    private final AuthSessionPersistencePort sessionPersistencePort;
    private final AuthActivityRecorder activityRecorder;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserPersistencePort userPersistencePort,
            AuthSessionPersistencePort sessionPersistencePort,
            AuthActivityRecorder activityRecorder,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            Clock clock) {
        this.userPersistencePort = userPersistencePort;
        this.sessionPersistencePort = sessionPersistencePort;
        this.activityRecorder = activityRecorder;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public UserAccount register(String email, String rawPassword) {
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
        activityRecorder.record(registered.getId(), normalizedEmail, "REGISTER", now);
        return registered;
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
        UUID sessionId = UUID.randomUUID();
        sessionPersistencePort.create(sessionId, account.getId(), now);
        List<String> roles = account.getRoles().stream().map(Enum::name).sorted().toList();
        IssuedAccessToken issued = tokenService.issue(account.getId(), account.getEmail(), sessionId, roles);
        activityRecorder.record(account.getId(), account.getEmail(), "LOGIN_SUCCESS", now);
        return issued;
    }

    public IssuedAccessToken refresh(Jwt currentToken) {
        UUID userId = UUID.fromString(currentToken.getClaimAsString("uid"));
        UUID sessionId = UUID.fromString(currentToken.getClaimAsString("sid"));
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
