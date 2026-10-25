package com.syncevent.participants.domain;

import java.util.List;

public record ParticipantSearchPage(List<Participant> content, long totalElements, int page, int size) {
}
