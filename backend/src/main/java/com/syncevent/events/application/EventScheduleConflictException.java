package com.syncevent.events.application;

public class EventScheduleConflictException extends RuntimeException {

    public EventScheduleConflictException() {
        super("El horario se cruza con otro evento programado en ese lugar.");
    }
}