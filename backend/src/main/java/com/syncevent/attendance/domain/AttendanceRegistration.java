package com.syncevent.attendance.domain;

import java.util.UUID;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.registrations.domain.RegistrationStatus;

public record AttendanceRegistration(
        UUID registrationId,
        UUID eventId,
        UUID participantId,
        String qrToken,
        RegistrationStatus registrationStatus,
        EstadoEvento eventStatus,
        String documentType,
        String documentNumber,
        String firstName,
        String lastName) {

    public String participantName() {
        return firstName + " " + lastName;
    }
}
