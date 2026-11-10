package com.syncevent.events.domain;

import java.time.LocalDate;
import java.util.Set;


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
