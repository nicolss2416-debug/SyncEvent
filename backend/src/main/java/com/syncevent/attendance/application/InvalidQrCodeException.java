package com.syncevent.attendance.application;

public class InvalidQrCodeException extends RuntimeException {

    public InvalidQrCodeException() {
        super("El código QR no corresponde a una inscripción válida para este evento.");
    }
}
