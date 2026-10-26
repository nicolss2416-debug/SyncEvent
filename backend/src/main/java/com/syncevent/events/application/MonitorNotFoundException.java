package com.syncevent.events.application;

import java.util.UUID;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(UUID monitorId) {
        super("No existe un monitor con identificador %s.".formatted(monitorId));
    }
}
