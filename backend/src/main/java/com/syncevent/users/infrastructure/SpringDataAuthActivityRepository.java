package com.syncevent.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuthActivityRepository extends JpaRepository<AuthActivityEntity, Long> {
}
