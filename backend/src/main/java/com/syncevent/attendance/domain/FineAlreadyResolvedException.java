package com.syncevent.attendance.domain;

import java.util.UUID;

public class FineAlreadyResolvedException extends RuntimeException {

    public FineAlreadyResolvedException(UUID fineId) {
        super("La multa %s ya fue resuelta.".formatted(fineId));
    }
}
