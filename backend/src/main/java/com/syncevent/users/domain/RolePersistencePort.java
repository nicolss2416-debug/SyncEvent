package com.syncevent.users.domain;

import java.util.List;

public interface RolePersistencePort {

    boolean existsByName(String roleName);

    List<RoleInfo> findAllRoles();

    void saveRole(RoleInfo roleInfo);
}
