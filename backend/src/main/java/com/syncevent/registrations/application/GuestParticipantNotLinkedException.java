package com.syncevent.registrations.application;

public class GuestParticipantNotLinkedException extends RuntimeException {

    public GuestParticipantNotLinkedException() {
        super("La cuenta Invitado no tiene un participante vinculado. Solicite la vinculación al Administrador.");
    }
}
