package com.syncevent.users.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.PasswordResetToken;
import com.syncevent.users.domain.PasswordResetTokenPersistencePort;

@Component
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenPersistencePort {

    private final SpringDataPasswordResetTokenRepository repository;

    public PasswordResetTokenPersistenceAdapter(SpringDataPasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                token.id(),
                token.userId(),
                token.tokenHash(),
                token.expiresAt(),
                token.used(),
                token.createdAt());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findActiveByTokenHash(String tokenHash, LocalDateTime now) {
        return repository.findActiveByTokenHash(tokenHash, now)
                .map(e -> new PasswordResetToken(
                        e.getId(),
                        e.getUserId(),
                        e.getTokenHash(),
                        e.getExpiresAt(),
                        e.isUsed(),
                        e.getCreatedAt()));
    }

    @Override
    @Transactional
    public void invalidateAllForUser(UUID userId, LocalDateTime now) {
        repository.invalidateAllForUser(userId);
    }
}
