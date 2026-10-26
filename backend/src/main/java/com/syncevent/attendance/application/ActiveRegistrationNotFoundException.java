package com.syncevent.attendance.application;

public class ActiveRegistrationNotFoundException extends RuntimeException {

    public ActiveRegistrationNotFoundException() {
        super("No existe una inscripción activa para el participante en este evento.");
    }
}
