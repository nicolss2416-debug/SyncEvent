package com.syncevent.users.api;

import jakarta.validation.constraints.NotNull;

public record SetUserActiveRequest(@NotNull Boolean active) {
}
