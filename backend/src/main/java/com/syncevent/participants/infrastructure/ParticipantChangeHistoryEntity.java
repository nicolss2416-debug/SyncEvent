package com.syncevent.participants.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "participant_change_history")
public class ParticipantChangeHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_id", nullable = false, updatable = false)
    private UUID participantId;

    @Column(name = "changed_by", nullable = false, updatable = false, length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected ParticipantChangeHistoryEntity() {
    }

    static ParticipantChangeHistoryEntity record(UUID participantId, String changedBy, LocalDateTime changedAt) {
        ParticipantChangeHistoryEntity history = new ParticipantChangeHistoryEntity();
        history.participantId = participantId;
        history.changedBy = changedBy;
        history.changedAt = changedAt;
        return history;
    }
}
