package com.syncevent.attendance.api;

import java.util.UUID;

import com.syncevent.attendance.domain.AttendanceCandidate;

public record AttendanceCandidateResponse(
        UUID registrationId,
        UUID participantId,
        String name,
        String documentType,
        String documentNumber) {

    static AttendanceCandidateResponse from(AttendanceCandidate candidate) {
        return new AttendanceCandidateResponse(
                candidate.registrationId(),
                candidate.participantId(),
                candidate.participantName(),
                candidate.documentType(),
                candidate.documentNumber());
    }
}
