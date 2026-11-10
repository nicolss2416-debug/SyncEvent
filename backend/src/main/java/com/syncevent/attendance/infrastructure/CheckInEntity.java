package com.syncevent.attendance.infrastructure;

import java.time.LocalDateTime;
import java.util.UUID;

import com.syncevent.attendance.domain.CheckIn;
import com.syncevent.attendance.domain.CheckInMethod;
import com.syncevent.registrations.infrastructure.RegistrationEntity;
import com.syncevent.users.infrastructure.UserAccountEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_attendance")
public class CheckInEntity {

    @Id
    @Column(name = "registration_id", nullable = false, updatable = false)
    private UUID registrationId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, insertable = false, updatable = false)
    private RegistrationEntity registration;

    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_in_by", nullable = false, updatable = false)
    private UUID checkedInBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_in_by", nullable = false, insertable = false, updatable = false)
    private UserAccountEntity checkedInByUser;

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
