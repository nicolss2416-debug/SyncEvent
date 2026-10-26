package com.syncevent.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccountEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserAccountEntity u WHERE u.id = :id")
    Optional<UserAccountEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByParticipant_Id(UUID participantId);

    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserAccountEntity u JOIN u.roles r
            WHERE u.active = true AND r = com.syncevent.users.domain.UserRole.ADMINISTRADOR
            """)
    long countActiveAdministrators();

    Page<UserAccountEntity> findAllByOrderByEmailAsc(Pageable pageable);
}
