package com.syncevent.users.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.syncevent.users.domain.UserAccount;

public record UserAccountResponse(
        UUID id,
        String email,
        List<String> roles,
        boolean active,
        LocalDateTime createdAt,
        UUID participantId) {

    public static UserAccountResponse from(UserAccount account) {
        return new UserAccountResponse(
                account.getId(),
                account.getEmail(),
                account.getRoles().stream().map(Enum::name).sorted().toList(),
                account.isActive(),
                account.getCreatedAt(),
                account.getParticipantId());
    }
}
