package com.syncevent.attendance.domain;

import java.util.UUID;

public record UnattendedRegistration(UUID registrationId, UUID participantId) {
}
