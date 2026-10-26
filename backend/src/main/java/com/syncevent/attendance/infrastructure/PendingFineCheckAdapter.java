package com.syncevent.attendance.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.registrations.domain.PendingFineCheckPort;

@Repository
public class PendingFineCheckAdapter implements PendingFineCheckPort {

    private final SpringDataParticipantFineRepository fineRepository;

    public PendingFineCheckAdapter(SpringDataParticipantFineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    @Override
    public boolean hasPendingFine(UUID participantId) {
        return fineRepository.existsByParticipantIdAndStatus(participantId, FineStatus.PENDIENTE);
    }
}
