package com.syncevent.attendance.application;

import com.syncevent.attendance.domain.AttendanceRegistration;
import com.syncevent.attendance.domain.CheckIn;

public record AttendanceCheckIn(AttendanceRegistration registration, CheckIn checkIn) {
}
