package com.syncevent.registrations.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.registrations.application.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events/{eventId}/registrations")
@Validated
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateRegistrationRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        RegistrationResponse response = RegistrationResponse.from(registrationService.register(
                eventId,
                request.participantId(),
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
        return ResponseEntity.created(URI.create(
                "/api/events/" + eventId + "/registrations/" + response.id())).body(response);
    }

    @PostMapping("/self")
    public ResponseEntity<RegistrationResponse> registerForGuest(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal Jwt currentToken) {
        RegistrationResponse response = RegistrationResponse.from(registrationService.registerForGuest(
                eventId,
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
        return ResponseEntity.created(URI.create(
                "/api/events/" + eventId + "/registrations/" + response.id())).body(response);
    }
}
