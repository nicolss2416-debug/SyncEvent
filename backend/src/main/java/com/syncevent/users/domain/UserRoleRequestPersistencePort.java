package com.syncevent.users.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRequestPersistencePort {

    UserRoleRequest save(UserRoleRequest request);

    List<UserRoleRequest> findPending();

    Optional<UserRoleRequest> findByIdForUpdate(UUID requestId);

    boolean existsPending(UUID userId, UserRole role);
}
