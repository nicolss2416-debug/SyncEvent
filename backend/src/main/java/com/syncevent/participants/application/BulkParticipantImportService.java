package com.syncevent.participants.application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncevent.participants.domain.Participant;
import com.syncevent.participants.domain.ParticipantPersistencePort;

/**
 * Caso de uso: importación atómica de un lote de participantes.
 * <p>
 * Política transaccional: si existe al menos un error de validación o documento
 * duplicado, se hace rollback completo del lote. El informe resultante refleja
 * siempre el estado real: o se guardaron todos los registros válidos o ninguno.
 */
@Service
public class BulkParticipantImportService {

    private final ParticipantPersistencePort persistencePort;

    public BulkParticipantImportService(ParticipantPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Transactional
    public BulkImportResult importBatch(List<RegisterParticipantCommand> commands) {
        List<BulkImportResult.RowError> errors = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        for (int i = 0; i < commands.size(); i++) {
            RegisterParticipantCommand command = commands.get(i);
            int rowNumber = i + 1;

            String documentType = command.documentType().trim().toUpperCase(Locale.ROOT);
            String documentNumber = command.documentNumber().trim().toUpperCase(Locale.ROOT);
            String batchKey = documentType + ":" + documentNumber;

            if (seenInBatch.contains(batchKey)) {
                errors.add(new BulkImportResult.RowError(rowNumber, command.documentNumber(),
                        "Documento duplicado dentro del mismo lote."));
                continue;
            }

            if (persistencePort.findByDocument(documentType, documentNumber).isPresent()) {
                errors.add(new BulkImportResult.RowError(rowNumber, command.documentNumber(),
                        "Ya existe un participante con ese tipo y número de documento."));
                continue;
            }

            seenInBatch.add(batchKey);
        }

        if (!errors.isEmpty()) {
            // Forzar rollback lanzando excepción luego de recopilar todos los errores.
            throw new BulkImportValidationException(errors, commands.size());
        }

        List<Participant> saved = new ArrayList<>();
        for (RegisterParticipantCommand command : commands) {
            String documentType = command.documentType().trim().toUpperCase(Locale.ROOT);
            String documentNumber = command.documentNumber().trim().toUpperCase(Locale.ROOT);
            saved.add(persistencePort.save(Participant.register(
                    documentType,
                    documentNumber,
                    command.firstName(),
                    command.lastName(),
                    command.email(),
                    command.phone(),
                    command.createdBy())));
        }

        return new BulkImportResult(saved, List.of());
    }
}
