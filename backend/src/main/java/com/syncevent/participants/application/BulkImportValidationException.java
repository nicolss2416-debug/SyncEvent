package com.syncevent.participants.application;

import java.util.List;

/**
 * Se lanza cuando el lote tiene errores de validación.
 * Al ser una RuntimeException no verificada, fuerza el rollback de la transacción.
 */
public class BulkImportValidationException extends RuntimeException {

    private final List<BulkImportResult.RowError> errors;
    private final int total;

    public BulkImportValidationException(List<BulkImportResult.RowError> errors, int total) {
        super("El lote contiene %d registro(s) con errores y no fue guardado.".formatted(errors.size()));
        this.errors = errors;
        this.total = total;
    }

    public List<BulkImportResult.RowError> getErrors() {
        return errors;
    }

    public int getTotal() {
        return total;
    }
}
