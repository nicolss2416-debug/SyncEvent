package com.syncevent.registrations.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.registrations.application.RegistrationService;

@RestController
@RequestMapping("/api/registrations")
@Validated
public class MyRegistrationsController {

    private final RegistrationService registrationService;

    public MyRegistrationsController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/mine")
    public List<MyRegistrationResponse> findMine(@AuthenticationPrincipal Jwt currentToken) {
        return registrationService.findMine(UUID.fromString(currentToken.getClaimAsString("uid")))
                .stream()
                .map(MyRegistrationResponse::from)
                .toList();
    }
}
