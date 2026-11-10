package com.syncevent.events.domain;

import java.util.List;


public record EventSearchPage(List<Event> content, long totalElements, int page, int size) {
}
