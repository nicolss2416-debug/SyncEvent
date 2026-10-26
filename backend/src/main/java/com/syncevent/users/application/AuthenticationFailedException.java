package com.syncevent.users.application;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Correo, contraseña o estado de cuenta inválidos.");
    }
}
