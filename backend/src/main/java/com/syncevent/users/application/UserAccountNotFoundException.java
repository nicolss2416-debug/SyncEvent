package com.syncevent.users.application;

import java.util.UUID;

public class UserAccountNotFoundException extends RuntimeException {

    public UserAccountNotFoundException(UUID userId) {
        super("No se encontró la cuenta de usuario " + userId + ".");
    }
}
