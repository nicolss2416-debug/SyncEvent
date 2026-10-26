package com.syncevent.attendance.domain;

import java.util.List;

public record FinePage(List<ParticipantFine> content, int page, int size, long totalElements) {
}
