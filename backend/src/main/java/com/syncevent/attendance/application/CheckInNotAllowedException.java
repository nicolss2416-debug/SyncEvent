package com.syncevent.attendance.application;

public class CheckInNotAllowedException extends RuntimeException {

    public CheckInNotAllowedException(String message) {
        super(message);
    }
}
