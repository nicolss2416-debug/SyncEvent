package com.syncevent.attendance.api;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.attendance.application.PenaltyAdministrationService;
import com.syncevent.attendance.domain.FineStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminPenaltyController {

    private final PenaltyAdministrationService administrationService;

    public AdminPenaltyController(PenaltyAdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @GetMapping("/attendance-policy")
    public AttendancePolicyResponse getPolicy() {
        return AttendancePolicyResponse.from(administrationService.getPolicy());
    }

    @PutMapping("/attendance-policy")
    public AttendancePolicyResponse updatePolicy(
            @Valid @RequestBody UpdateAttendancePolicyRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        return AttendancePolicyResponse.from(administrationService.updatePolicy(
                request.absenceThreshold(),
                request.windowDays(),
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
    }

    @GetMapping("/fines")
    public FinePageResponse findFines(
            @RequestParam(required = false) FineStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return FinePageResponse.from(administrationService.searchFines(status, page, size));
    }

    @PostMapping("/fines/{fineId}/resolution")
    public ParticipantFineResponse resolveFine(
            @PathVariable UUID fineId,
            @AuthenticationPrincipal Jwt currentToken) {
        return ParticipantFineResponse.from(administrationService.resolveFine(
                fineId,
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
    }
}
