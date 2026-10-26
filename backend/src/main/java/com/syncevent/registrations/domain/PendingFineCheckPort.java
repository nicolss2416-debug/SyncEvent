package com.syncevent.registrations.domain;

import java.util.UUID;

public interface PendingFineCheckPort {

    boolean hasPendingFine(UUID participantId);
}
