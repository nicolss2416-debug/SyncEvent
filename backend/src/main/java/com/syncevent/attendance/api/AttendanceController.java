package com.syncevent.attendance.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.attendance.application.AttendanceService;
import com.syncevent.attendance.domain.AttendanceCandidate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/events/{eventId}/attendance")
@Validated
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/qr")
    public ResponseEntity<AttendanceResponse> checkInByQr(
            @PathVariable UUID eventId,
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        AttendanceResponse response = AttendanceResponse.from(attendanceService.checkInByQr(
                eventId,
                request.qrToken(),
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/registrations")
    public List<AttendanceCandidateResponse> searchRegistrations(
            @PathVariable UUID eventId,
            @RequestParam @NotBlank @Size(max = 100) String q) {
        return attendanceService.searchRegistrations(eventId, q).stream()
                .map(AttendanceCandidateResponse::from)
                .toList();
    }

    @PostMapping("/manual")
    public ResponseEntity<AttendanceResponse> checkInManually(
            @PathVariable UUID eventId,
            @Valid @RequestBody ManualCheckInRequest request,
            @AuthenticationPrincipal Jwt currentToken) {
        AttendanceResponse response = AttendanceResponse.from(attendanceService.checkInManually(
                eventId,
                request.participantId(),
                UUID.fromString(currentToken.getClaimAsString("uid")),
                currentToken.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
