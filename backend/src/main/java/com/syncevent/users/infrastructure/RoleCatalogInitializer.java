package com.syncevent.users.infrastructure;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.users.domain.RoleInfo;
import com.syncevent.users.domain.RolePersistencePort;
import com.syncevent.users.domain.UserRole;

@Component
@Order(1)
public class RoleCatalogInitializer implements CommandLineRunner {

    private final RolePersistencePort rolePersistencePort;

    public RoleCatalogInitializer(RolePersistencePort rolePersistencePort) {
        this.rolePersistencePort = rolePersistencePort;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<RoleInfo> defaults = List.of(
                new RoleInfo(UserRole.ADMINISTRADOR.name(), "Administrador", "Gestión total del sistema, configuración, cuentas y auditoría."),
                new RoleInfo(UserRole.OPERADOR.name(), "Operador", "Creación y administración de eventos, inscripciones y reportes."),
                new RoleInfo(UserRole.MONITOR.name(), "Monitor", "Control de asistencia y verificación en campo."),
                new RoleInfo(UserRole.INVITADO.name(), "Invitado", "Visualización del catálogo de eventos y gestión de su propia inscripción.")
        );

        for (RoleInfo roleInfo : defaults) {
            if (!rolePersistencePort.existsByName(roleInfo.name())) {
                rolePersistencePort.saveRole(roleInfo);
            }
        }
    }
}
