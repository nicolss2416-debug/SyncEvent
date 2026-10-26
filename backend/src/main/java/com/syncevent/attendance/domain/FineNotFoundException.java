package com.syncevent.attendance.domain;

import java.util.UUID;

public class FineNotFoundException extends RuntimeException {

    public FineNotFoundException(UUID fineId) {
        super("No existe una multa con identificador %s.".formatted(fineId));
    }
}
