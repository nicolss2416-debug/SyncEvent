package com.syncevent.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRoleRepository extends JpaRepository<RoleEntity, String> {

    boolean existsByName(String name);
}
