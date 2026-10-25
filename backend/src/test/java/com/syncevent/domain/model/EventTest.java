package com.syncevent.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void creaEventoEnBorrador() {
        Event event = Event.create(
                "Capacitación Java",
                "Descripción",
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2027, 3, 1, 9, 0),
                LocalDateTime.of(2027, 3, 1, 12, 0),
                "Sede central",
                null,
                30,
                new BigDecimal("3.00"));

        assertEquals(EstadoEvento.BORRADOR, event.getStatus());
    }

    @Test
    void rechazaFinAnteriorAlInicio() {
        assertThrows(IllegalArgumentException.class, () -> Event.create(
                "Capacitación Java",
                null,
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2027, 3, 1, 12, 0),
                LocalDateTime.of(2027, 3, 1, 9, 0),
                "Sede central",
                null,
                30,
                new BigDecimal("3.00")));
    }

    @Test
    void exigeLugarParaEventoPresencial() {
        assertThrows(IllegalArgumentException.class, () -> Event.create(
                "Capacitación Java",
                null,
                TipoEvento.CAPACITACION,
                Modalidad.PRESENCIAL,
                LocalDateTime.of(2027, 3, 1, 9, 0),
                LocalDateTime.of(2027, 3, 1, 12, 0),
                null,
                null,
                30,
                new BigDecimal("3.00")));
    }
}
