package com.syncevent.users.api;

import com.syncevent.users.domain.UserRole;

public record UserRoleResponse(
        String role,
        String name,
        String description) {

    public static UserRoleResponse from(UserRole role) {
        return switch (role) {
            case ADMINISTRADOR -> new UserRoleResponse(
                    role.name(),
                    "Administrador",
                    "Gestión total del sistema, configuración, cuentas y auditoría.");
            case OPERADOR -> new UserRoleResponse(
                    role.name(),
                    "Operador",
                    "Creación y administración de eventos, inscripciones y reportes.");
            case MONITOR -> new UserRoleResponse(
                    role.name(),
                    "Monitor",
                    "Control de asistencia y verificación en campo.");
            case INVITADO -> new UserRoleResponse(
                    role.name(),
                    "Invitado",
                    "Visualización del catálogo de eventos y gestión de su propia inscripción.");
        };
    }
}
