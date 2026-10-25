package com.syncevent.events.domain.port;

import java.time.LocalDate;
import java.util.Set;

import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.domain.model.Modalidad;
import com.syncevent.domain.model.TipoEvento;

public record EventSearchCriteria(
        TipoEvento type,
        Modalidad modality,
        EstadoEvento status,
        LocalDate from,
        LocalDate to,
        Set<EstadoEvento> visibleStatuses,
        int page,
        int size,
        EventSort sort,
        SortDirection direction) {
}
