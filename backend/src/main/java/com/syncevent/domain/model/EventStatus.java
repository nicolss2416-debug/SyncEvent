package com.syncevent.domain.model;

/**
 * Estado del ciclo de vida de un {@link Event}.
 */
public enum EventStatus {

    /** Evento creado pero aún no publicado. */
    DRAFT,

    /** Evento publicado y visible para los asistentes. */
    PUBLISHED,

    /** Evento cancelado; no admite nuevas inscripciones. */
    CANCELLED,

    /** Evento finalizado. */
    FINISHED
}
