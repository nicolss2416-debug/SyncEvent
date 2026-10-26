package com.syncevent.registrations.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.syncevent.attendance.domain.AttendanceCandidate;
import com.syncevent.attendance.domain.AttendancePersistencePort;
import com.syncevent.attendance.domain.AttendanceRegistration;
import com.syncevent.attendance.domain.CheckIn;
import com.syncevent.attendance.infrastructure.CheckInEntity;
import com.syncevent.attendance.infrastructure.SpringDataCheckInRepository;

@Repository
public class AttendancePersistenceAdapter implements AttendancePersistencePort {

    private final SpringDataRegistrationRepository registrationRepository;
    private final SpringDataCheckInRepository checkInRepository;

    public AttendancePersistenceAdapter(
            SpringDataRegistrationRepository registrationRepository,
            SpringDataCheckInRepository checkInRepository) {
        this.registrationRepository = registrationRepository;
        this.checkInRepository = checkInRepository;
    }

    @Override
    public Optional<AttendanceRegistration> findByQrTokenForUpdate(String qrToken) {
        return registrationRepository.findByQrTokenForUpdate(qrToken)
                .map(RegistrationEntity::toAttendanceRegistration);
    }

    @Override
    public Optional<AttendanceRegistration> findActiveByEventAndParticipantForUpdate(
            UUID eventId, UUID participantId) {
        return registrationRepository.findActiveByEventAndParticipantForUpdate(eventId, participantId)
                .map(RegistrationEntity::toAttendanceRegistration);
    }

    @Override
    public List<AttendanceCandidate> searchActiveRegistrations(UUID eventId, String query, int limit) {
        return registrationRepository.searchActiveByEvent(eventId, query, PageRequest.of(0, limit))
                .getContent()
                .stream()
                .map(RegistrationEntity::toAttendanceCandidate)
                .toList();
    }

    @Override
    public Optional<CheckIn> findByRegistrationId(UUID registrationId) {
        return checkInRepository.findById(registrationId).map(CheckInEntity::toDomain);
    }

    @Override
    public CheckIn save(CheckIn checkIn) {
        return checkInRepository.saveAndFlush(CheckInEntity.fromDomain(checkIn)).toDomain();
    }
}
