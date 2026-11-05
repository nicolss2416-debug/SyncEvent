package com.syncevent.users.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.users.domain.RoleRequestStatus;
import com.syncevent.users.domain.UserRole;
import com.syncevent.users.domain.UserRoleRequest;
import com.syncevent.users.domain.UserRoleRequestPersistencePort;

@Repository
public class UserRoleRequestPersistenceAdapter implements UserRoleRequestPersistencePort {

    private final SpringDataUserRoleRequestRepository repository;

    public UserRoleRequestPersistenceAdapter(SpringDataUserRoleRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserRoleRequest save(UserRoleRequest request) {
        return repository.save(UserRoleRequestEntity.fromDomain(request)).toDomain();
    }

    @Override
    public List<UserRoleRequest> findPending() {
        return repository.findAllByStatusOrderByRequestedAtAsc(RoleRequestStatus.PENDIENTE).stream()
                .map(UserRoleRequestEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<UserRoleRequest> findByIdForUpdate(UUID requestId) {
        return repository.findByIdForUpdate(requestId).map(UserRoleRequestEntity::toDomain);
    }

    @Override
    public boolean existsPending(UUID userId, UserRole role) {
        return repository.existsByUserIdAndRequestedRoleAndStatus(
                userId, role, RoleRequestStatus.PENDIENTE);
    }
}
