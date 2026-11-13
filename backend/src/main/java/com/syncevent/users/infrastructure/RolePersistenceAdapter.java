package com.syncevent.users.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.syncevent.users.domain.RoleInfo;
import com.syncevent.users.domain.RolePersistencePort;

@Repository
public class RolePersistenceAdapter implements RolePersistencePort {

    private final SpringDataRoleRepository repository;

    public RolePersistenceAdapter(SpringDataRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        return repository.existsByName(roleName.trim());
    }

    @Override
    public List<RoleInfo> findAllRoles() {
        return repository.findAll().stream()
                .map(RoleEntity::toDomain)
                .toList();
    }

    @Override
    public void saveRole(RoleInfo roleInfo) {
        repository.saveAndFlush(RoleEntity.fromDomain(roleInfo));
    }
}
