package com.syncevent.participants.infrastructure;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.syncevent.attendance.domain.FineStatus;
import com.syncevent.attendance.domain.CheckInMethod;
import com.syncevent.domain.model.EstadoEvento;
import com.syncevent.participants.domain.ParticipantHistoryEntry;
import com.syncevent.registrations.domain.RegistrationStatus;
import com.syncevent.registrations.infrastructure.RegistrationEntity;

interface SpringDataParticipantHistoryRepository extends Repository<RegistrationEntity, UUID> {

    @Query(
            value = """
                    SELECT new com.syncevent.participants.domain.ParticipantHistoryEntry(
                        r.id,
                        e.id,
                        e.name,
                        e.startAt,
                        e.status,
                        r.status,
                        c.checkedInAt,
                        c.method,
                        a.recordedAt,
                        f.id,
                        f.status,
                        f.createdAt,
                        f.resolvedAt,
                        cancellation.reason)
                    FROM RegistrationEntity r
                    JOIN r.event e
                    LEFT JOIN CheckInEntity c ON c.registrationId = r.id
                    LEFT JOIN AttendanceAbsenceEntity a ON a.registrationId = r.id
                    LEFT JOIN ParticipantFineEntity f ON f.triggeredByAbsenceId = a.id
                    LEFT JOIN RegistrationCancellationHistoryEntity cancellation
                        ON cancellation.registrationId = r.id
                    WHERE r.participant.id = :participantId
                    ORDER BY e.startAt DESC, r.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(r.id)
                    FROM RegistrationEntity r
                    WHERE r.participant.id = :participantId
                    """)
    Page<ParticipantHistoryEntry> findHistory(
            @Param("participantId") UUID participantId, Pageable pageable);
}
