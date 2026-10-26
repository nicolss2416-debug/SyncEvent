package com.syncevent.attendance.application;

import java.time.LocalDateTime;

public class CheckInAlreadyRecordedException extends RuntimeException {

    private final LocalDateTime checkedInAt;

    public CheckInAlreadyRecordedException(LocalDateTime checkedInAt) {
        super("La asistencia ya fue registrada a las " + checkedInAt + ".");
        this.checkedInAt = checkedInAt;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
}
