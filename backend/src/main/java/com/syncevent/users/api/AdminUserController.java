package com.syncevent.users.api;

import java.util.UUID;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.users.application.AdminUserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/users")
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> create(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        UserAccountResponse response = UserAccountResponse.from(adminUserService.create(
                request.email(),
                request.password(),
                request.roles(),
                UUID.fromString(currentToken.getClaimAsString("uid"))));
        return ResponseEntity.created(URI.create("/api/admin/users/" + response.id()))
                .body(response);
    }

    @GetMapping
    public UserAccountPageResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return UserAccountPageResponse.from(adminUserService.findAll(page, size));
    }

    @GetMapping("/{userId}")
    public UserAccountResponse findById(@PathVariable UUID userId) {
        return UserAccountResponse.from(adminUserService.findById(userId));
    }

    @PutMapping("/{userId}")
    public UserAccountResponse updateEmail(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserEmailRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return UserAccountResponse.from(
                adminUserService.updateEmail(
                        userId, request.email(), UUID.fromString(currentToken.getClaimAsString("uid"))));
    }

    @PostMapping("/{userId}/roles")
    public UserAccountResponse assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AssignUserRoleRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return UserAccountResponse.from(
                adminUserService.assignRole(
                        userId, request.role(), UUID.fromString(currentToken.getClaimAsString("uid"))));
    }

    @PatchMapping("/{userId}/active")
    public UserAccountResponse setActive(
            @PathVariable UUID userId,
            @Valid @RequestBody SetUserActiveRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return UserAccountResponse.from(
                adminUserService.setActive(
                        userId, request.active(), UUID.fromString(currentToken.getClaimAsString("uid"))));
    }
}
