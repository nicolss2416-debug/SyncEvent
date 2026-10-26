package com.syncevent.users.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.syncevent.users.domain.UserAccount;

public record RegisteredUserResponse(
        UUID id,
        String email,
        List<String> roles,
        LocalDateTime createdAt) {

    public static RegisteredUserResponse from(UserAccount account) {
        return new RegisteredUserResponse(
                account.getId(),
                account.getEmail(),
                account.getRoles().stream().map(Enum::name).sorted().toList(),
                account.getCreatedAt());
    }
}
