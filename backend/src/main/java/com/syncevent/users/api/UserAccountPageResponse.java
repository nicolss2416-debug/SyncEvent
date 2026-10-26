package com.syncevent.users.api;

import java.util.List;

import com.syncevent.users.domain.UserAccountSearchPage;

public record UserAccountPageResponse(
        List<UserAccountResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static UserAccountPageResponse from(UserAccountSearchPage page) {
        return new UserAccountPageResponse(
                page.content().stream().map(UserAccountResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
