package com.syncevent.users.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncevent.users.domain.RoleRequestStatus;
import com.syncevent.users.domain.UserRole;

import jakarta.persistence.LockModeType;

interface SpringDataUserRoleRequestRepository extends JpaRepository<UserRoleRequestEntity, UUID> {

    List<UserRoleRequestEntity> findAllByStatusOrderByRequestedAtAsc(RoleRequestStatus status);

    boolean existsByUserIdAndRequestedRoleAndStatus(
            UUID userId, UserRole requestedRole, RoleRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM UserRoleRequestEntity request WHERE request.id = :id")
    Optional<UserRoleRequestEntity> findByIdForUpdate(@Param("id") UUID id);
}
