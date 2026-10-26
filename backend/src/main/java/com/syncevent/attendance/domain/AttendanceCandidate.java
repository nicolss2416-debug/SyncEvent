package com.syncevent.attendance.domain;

import java.util.UUID;

public record AttendanceCandidate(
        UUID registrationId,
        UUID participantId,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName) {

    public String participantName() {
        return firstName + " " + lastName;
    }
}
