package com.syncevent.users.domain;

import java.util.List;

public record UserAccountSearchPage(
        List<UserAccount> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public UserAccountSearchPage {
        content = List.copyOf(content);
    }
}
