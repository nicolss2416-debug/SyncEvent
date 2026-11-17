package com.syncevent.users.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.users.application.AuthService;
import com.syncevent.users.application.IssuedAccessToken;
import com.syncevent.participants.api.ParticipantResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        RegisteredUserResponse response = RegisteredUserResponse.from(
            authService.register(
                request.email(),
                request.password(),
                request.hasGuestProfile() ? request.toGuestProfileCommand() : null));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/profile")
    public ParticipantResponse completeGuestProfile(
            @Valid @RequestBody GuestProfileRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return ParticipantResponse.from(authService.completeGuestProfile(
            java.util.UUID.fromString(currentToken.getClaimAsString("uid")),
            currentToken.getSubject(),
            request.toCommand()));
    }

    @PostMapping("/login")
    public IssuedAccessToken login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return authService.login(request.email(), request.password(), resolveClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    public IssuedAccessToken refresh(@AuthenticationPrincipal Jwt currentToken) {
        return authService.refresh(currentToken);
    }

    @GetMapping("/me")
    public IssuedAccessToken currentSession(@AuthenticationPrincipal Jwt currentToken) {
        return authService.refresh(currentToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt currentToken) {
        authService.logout(currentToken);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        authService.changePassword(
                java.util.UUID.fromString(currentToken.getClaimAsString("uid")),
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        com.syncevent.users.application.ForgotPasswordResult result =
                authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(new ForgotPasswordResponse(result.message(), result.devOtp()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordWithOtpRequest request,
            HttpServletRequest httpRequest) {
        authService.resetPasswordWithOtp(
                request.email(), request.otp(), request.newPassword(), resolveClientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "127.0.0.1";
    }
}
