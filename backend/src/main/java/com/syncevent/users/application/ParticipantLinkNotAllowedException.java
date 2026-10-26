package com.syncevent.users.application;

public class ParticipantLinkNotAllowedException extends RuntimeException {

    public ParticipantLinkNotAllowedException(String message) {
        super(message);
    }
}
