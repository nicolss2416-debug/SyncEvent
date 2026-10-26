package com.syncevent.users.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccountEntity> findByEmail(String email);

    @Query("""
            SELECT COUNT(DISTINCT u) FROM UserAccountEntity u JOIN u.roles r
            WHERE u.active = true AND r = com.syncevent.users.domain.UserRole.ADMINISTRADOR
            """)
    long countActiveAdministrators();

    Page<UserAccountEntity> findAllByOrderByEmailAsc(Pageable pageable);
}
