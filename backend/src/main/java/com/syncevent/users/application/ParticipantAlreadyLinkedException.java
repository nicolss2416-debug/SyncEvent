package com.syncevent.users.application;

public class ParticipantAlreadyLinkedException extends RuntimeException {

    public ParticipantAlreadyLinkedException(String message) {
        super(message);
    }
}
