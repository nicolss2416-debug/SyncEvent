package com.syncevent.users.application;

public record ForgotPasswordResult(
        boolean success,
        String message,
        String devOtp) {
}
