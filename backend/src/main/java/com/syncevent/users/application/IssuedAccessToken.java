package com.syncevent.users.application;

import java.time.Duration;
import java.util.List;

public record IssuedAccessToken(String accessToken, String tokenType, long expiresIn, List<String> roles) {

    public static IssuedAccessToken bearer(String token, Duration timeToLive, List<String> roles) {
        return new IssuedAccessToken(token, "Bearer", timeToLive.toSeconds(), List.copyOf(roles));
    }
}
