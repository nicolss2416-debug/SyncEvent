package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.attendance.domain.AttendanceAbsence;
import com.syncevent.attendance.domain.FinePage;
import com.syncevent.attendance.domain.FinePolicy;
import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.attendance.domain.ParticipantFine;
import com.syncevent.attendance.domain.PenaltyPersistencePort;
import com.syncevent.attendance.domain.UnattendedRegistration;
import com.syncevent.registrations.domain.RegistrationStatus;

@Repository
public class PenaltyPersistenceAdapter implements PenaltyPersistencePort {

    private final SpringDataUnattendedRegistrationRepository unattendedRegistrationRepository;
    private final SpringDataAttendanceAbsenceRepository absenceRepository;
    private final SpringDataAttendancePolicyRepository policyRepository;
    private final SpringDataParticipantFineRepository fineRepository;

    public PenaltyPersistenceAdapter(
            SpringDataUnattendedRegistrationRepository unattendedRegistrationRepository,
            SpringDataAttendanceAbsenceRepository absenceRepository,
            SpringDataAttendancePolicyRepository policyRepository,
            SpringDataParticipantFineRepository fineRepository) {
        this.unattendedRegistrationRepository = unattendedRegistrationRepository;
        this.absenceRepository = absenceRepository;
        this.policyRepository = policyRepository;
        this.fineRepository = fineRepository;
    }

    @Override
    public List<UnattendedRegistration> findActiveRegistrationsWithoutCheckIn(UUID eventId) {
        return unattendedRegistrationRepository.findActiveWithoutCheckIn(eventId, RegistrationStatus.ACTIVA);
    }

    @Override
    public boolean existsAbsenceByRegistrationId(UUID registrationId) {
        return absenceRepository.existsByRegistrationId(registrationId);
    }

    @Override
    public AttendanceAbsence saveAbsence(AttendanceAbsence absence) {
        return absenceRepository.saveAndFlush(AttendanceAbsenceEntity.fromDomain(absence)).toDomain();
    }

    @Override
    public long countAbsencesSince(UUID participantId, LocalDateTime since) {
        return absenceRepository.countByParticipantIdAndRecordedAtGreaterThanEqual(participantId, since);
    }

    @Override
    public FinePolicy findPolicy() {
        return policyRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException("No se encontró la política de inasistencias."))
                .toDomain();
    }

    @Override
    public FinePolicy findPolicyForUpdate() {
        return policyRepository.findPolicyForUpdate()
                .orElseThrow(() -> new IllegalStateException("No se encontró la política de inasistencias."))
                .toDomain();
    }

    @Override
    public FinePolicy savePolicy(FinePolicy policy) {
        AttendancePolicyEntity entity = policyRepository.findPolicyForUpdate()
                .orElseThrow(() -> new IllegalStateException("No se encontró la política de inasistencias."));
        entity.update(policy);
        return policyRepository.saveAndFlush(entity).toDomain();
    }

    @Override
    public boolean existsPendingFine(UUID participantId) {
        return fineRepository.existsByParticipantIdAndStatus(participantId, FineStatus.PENDIENTE);
    }

    @Override
    public Optional<ParticipantFine> findFineByIdForUpdate(UUID fineId) {
        return fineRepository.findByIdForUpdate(fineId).map(ParticipantFineEntity::toDomain);
    }

    @Override
    public FinePage searchFines(FineStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ParticipantFineEntity> result = status == null
                ? fineRepository.findAllByOrderByCreatedAtDescIdAsc(pageable)
                : fineRepository.findAllByStatusOrderByCreatedAtDescIdAsc(status, pageable);
        return new FinePage(
                result.getContent().stream().map(ParticipantFineEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    public ParticipantFine saveFine(ParticipantFine fine) {
        return fineRepository.saveAndFlush(ParticipantFineEntity.fromDomain(fine)).toDomain();
    }
}
