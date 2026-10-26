package com.syncevent.users.domain;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserPersistencePort {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);

    Optional<UserAccount> findByIdForUpdate(UUID id);

    List<UserAccount> findMonitors();

    boolean existsByParticipantId(UUID participantId);

    UserAccountSearchPage findAll(int page, int size);

    long countActiveAdministrators();

    UserAccount save(UserAccount account);
}
