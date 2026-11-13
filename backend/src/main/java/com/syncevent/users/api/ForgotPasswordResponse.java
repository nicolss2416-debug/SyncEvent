package com.syncevent.users.api;

public record ForgotPasswordResponse(
        String message,
        String devOtp) {
}
