package com.syncevent.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserAccountSearchPage;
import com.syncevent.users.domain.UserPersistencePort;

@Repository
public class UserPersistenceAdapter implements UserPersistencePort {

    private final SpringDataUserAccountRepository repository;

    public UserPersistenceAdapter(SpringDataUserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(UserAccountEntity::toDomain);
    }

    @Override
    public UserAccountSearchPage findAll(int page, int size) {
        Page<UserAccountEntity> result = repository.findAllByOrderByEmailAsc(PageRequest.of(page, size));
        return new UserAccountSearchPage(
                result.getContent().stream().map(UserAccountEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public long countActiveAdministrators() {
        return repository.countActiveAdministrators();
    }

    @Override
    public UserAccount save(UserAccount account) {
        return repository.saveAndFlush(UserAccountEntity.fromDomain(account)).toDomain();
    }
}
