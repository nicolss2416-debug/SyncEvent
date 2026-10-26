package com.syncevent.attendance.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface SpringDataAttendancePolicyRepository
        extends JpaRepository<AttendancePolicyEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM AttendancePolicyEntity p WHERE p.id = 1")
    Optional<AttendancePolicyEntity> findPolicyForUpdate();
}
