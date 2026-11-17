package com.syncevent.users.domain;

public interface PasswordResetNotificationPort {

    void sendPasswordResetOtp(String email, String otp);
}
