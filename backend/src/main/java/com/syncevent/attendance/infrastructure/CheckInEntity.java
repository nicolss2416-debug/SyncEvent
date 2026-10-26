package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.CheckIn;
import com.syncevent.attendance.domain.CheckInMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_attendance")
public class CheckInEntity {

    @Id
    @Column(name = "registration_id", nullable = false, updatable = false)
    private UUID registrationId;

    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_in_by", nullable = false, updatable = false)
    private UUID checkedInBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private CheckInMethod method;

    protected CheckInEntity() {
    }

    public static CheckInEntity fromDomain(CheckIn checkIn) {
        CheckInEntity entity = new CheckInEntity();
        entity.registrationId = checkIn.registrationId();
        entity.checkedInAt = checkIn.checkedInAt();
        entity.checkedInBy = checkIn.checkedInBy();
        entity.method = checkIn.method();
        return entity;
    }

    public CheckIn toDomain() {
        return new CheckIn(registrationId, checkedInAt, checkedInBy, method);
    }
}
