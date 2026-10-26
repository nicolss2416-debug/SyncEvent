package com.syncevent.attendance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckInRequest(@NotBlank @Size(max = 64) String qrToken) {
}
