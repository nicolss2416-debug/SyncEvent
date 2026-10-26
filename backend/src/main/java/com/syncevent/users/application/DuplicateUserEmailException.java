package com.syncevent.users.application;

public class DuplicateUserEmailException extends RuntimeException {

    public DuplicateUserEmailException() {
        super("Ya existe una cuenta registrada con ese correo.");
    }
}
