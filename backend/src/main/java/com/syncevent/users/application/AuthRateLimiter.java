package com.syncevent.users.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

    public static final int MAX_LOGIN_ATTEMPTS_PER_IP = 10;
    public static final int MAX_LOGIN_ATTEMPTS_PER_ACCOUNT = 5;
    public static final int MAX_RESET_ATTEMPTS_PER_IP = 5;
    public static final int MAX_RESET_ATTEMPTS_PER_ACCOUNT = 5;
    public static final Duration WINDOW_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> loginAttemptsByIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> loginAttemptsByEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> resetAttemptsByIp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> resetAttemptsByEmail = new ConcurrentHashMap<>();

    public AuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void checkLoginAllowed(String ip, String email) {
        Instant now = Instant.now(clock);
        if (isBlocked(loginAttemptsByIp, normalizeIp(ip), MAX_LOGIN_ATTEMPTS_PER_IP, now)) {
            throw new RateLimitExceededException(
                    "Demasiados intentos fallidos desde esta dirección IP. Inténtalo de nuevo en 15 minutos.");
        }
        if (email != null && !email.isBlank()) {
            if (isBlocked(loginAttemptsByEmail, normalizeEmail(email), MAX_LOGIN_ATTEMPTS_PER_ACCOUNT, now)) {
                throw new RateLimitExceededException(
                        "Demasiados intentos fallidos para esta cuenta. Por seguridad, la cuenta está bloqueada temporalmente por 15 minutos.");
            }
        }
    }

    public void recordLoginFailure(String ip, String email) {
        Instant now = Instant.now(clock);
        recordAttempt(loginAttemptsByIp, normalizeIp(ip), now);
        if (email != null && !email.isBlank()) {
            recordAttempt(loginAttemptsByEmail, normalizeEmail(email), now);
        }
    }

    public void recordLoginSuccess(String ip, String email) {
        if (email != null && !email.isBlank()) {
            loginAttemptsByEmail.remove(normalizeEmail(email));
        }
    }

    public void checkResetPasswordAllowed(String ip, String email) {
        Instant now = Instant.now(clock);
        if (isBlocked(resetAttemptsByIp, normalizeIp(ip), MAX_RESET_ATTEMPTS_PER_IP, now)) {
            throw new RateLimitExceededException(
                    "Demasiados intentos de restablecimiento desde esta dirección IP. Inténtalo de nuevo en 15 minutos.");
        }
        if (email != null && !email.isBlank()) {
            if (isBlocked(resetAttemptsByEmail, normalizeEmail(email), MAX_RESET_ATTEMPTS_PER_ACCOUNT, now)) {
                throw new RateLimitExceededException(
                        "Demasiados intentos de restablecimiento para esta cuenta. Inténtalo de nuevo en 15 minutos.");
            }
        }
    }

    public void recordResetPasswordFailure(String ip, String email) {
        Instant now = Instant.now(clock);
        recordAttempt(resetAttemptsByIp, normalizeIp(ip), now);
        if (email != null && !email.isBlank()) {
            recordAttempt(resetAttemptsByEmail, normalizeEmail(email), now);
        }
    }

    public void recordResetPasswordSuccess(String ip, String email) {
        if (email != null && !email.isBlank()) {
            resetAttemptsByEmail.remove(normalizeEmail(email));
        }
    }

    public void clearAll() {
        loginAttemptsByIp.clear();
        loginAttemptsByEmail.clear();
        resetAttemptsByIp.clear();
        resetAttemptsByEmail.clear();
    }

    private boolean isBlocked(ConcurrentHashMap<String, Deque<Instant>> map, String key, int maxAttempts, Instant now) {
        Deque<Instant> attempts = map.get(key);
        if (attempts == null) {
            return false;
        }
        cleanExpired(attempts, now);
        return attempts.size() >= maxAttempts;
    }

    private void recordAttempt(ConcurrentHashMap<String, Deque<Instant>> map, String key, Instant now) {
        Deque<Instant> attempts = map.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        cleanExpired(attempts, now);
        attempts.addLast(now);
    }

    private void cleanExpired(Deque<Instant> attempts, Instant now) {
        Instant threshold = now.minus(WINDOW_DURATION);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
            attempts.pollFirst();
        }
    }

    private String normalizeIp(String ip) {
        return (ip == null || ip.isBlank()) ? "unknown" : ip.trim();
    }

    private String normalizeEmail(String email) {
        return (email == null) ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
