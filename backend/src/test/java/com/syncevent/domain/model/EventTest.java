package com.syncevent.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class EventTest {

    private Event crearEventoValido() {
        return new Event(
                "Conferencia Java",
                "Evento de prueba",
                LocalDateTime.now().plusDays(10),
                "Bogotá",
                100);
    }

    @Test
    void creaEventoEnEstadoBorrador() {
        Event event = crearEventoValido();

        assertEquals(EventStatus.DRAFT, event.getStatus());
        assertTrue(event.getAttendeeIds().isEmpty());
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> new Event(
                "  ",
                null,
                LocalDateTime.now().plusDays(1),
                null,
                10));
    }

    @Test
    void rechazaAforoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Event(
                "Evento",
                null,
                LocalDateTime.now().plusDays(1),
                null,
                0));
    }

    @Test
    void rechazaFechaNula() {
        assertThrows(NullPointerException.class, () -> new Event(
                "Evento",
                null,
                null,
                null,
                10));
    }

    @Test
    void noPermiteModificarLaListaDeAsistentesDesdeElExterior() {
        Event event = crearEventoValido();

        assertThrows(UnsupportedOperationException.class,
                () -> event.getAttendeeIds().add("asistente-1"));
    }

    @Test
    void almacenaUnaCopiaDefensivaDeLosAsistentes() {
        Event event = crearEventoValido();
        List<String> asistentes = new ArrayList<>(List.of("asistente-1"));

        event.setAttendeeIds(asistentes);
        asistentes.add("asistente-2");

        assertEquals(List.of("asistente-1"), event.getAttendeeIds());
    }
}
