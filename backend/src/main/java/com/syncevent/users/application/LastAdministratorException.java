package com.syncevent.users.application;

public class LastAdministratorException extends RuntimeException {

    public LastAdministratorException() {
        super("No se puede desactivar al último Administrador activo.");
    }
}
