package com.syncevent.participants.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.participants.application.ParticipantService;
import com.syncevent.participants.application.ParticipantHistoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/participants")
@Validated
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantHistoryService participantHistoryService;

    public ParticipantController(
            ParticipantService participantService,
            ParticipantHistoryService participantHistoryService) {
        this.participantService = participantService;
        this.participantHistoryService = participantHistoryService;
    }

    @PostMapping
    public ResponseEntity<ParticipantResponse> register(
            @Valid @RequestBody RegisterParticipantRequest request,
            Authentication authentication) {
        ParticipantResponse response = ParticipantResponse.from(
                participantService.register(request.toCommand(authentication.getName())));
        return ResponseEntity.created(URI.create("/api/participants/" + response.id())).body(response);
    }

    @GetMapping
    public ParticipantPageResponse search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ParticipantPageResponse.from(participantService.search(query, page, size));
    }

    @GetMapping("/{participantId}")
    public ParticipantResponse findById(@PathVariable UUID participantId) {
        return ParticipantResponse.from(participantService.findById(participantId));
    }

    @GetMapping("/{participantId}/history")
    public ParticipantHistoryResponse findHistory(
            @PathVariable UUID participantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ParticipantHistoryResponse.from(
                participantId,
                participantHistoryService.findHistory(participantId, page, size));
    }

    @PutMapping("/{participantId}")
    public ParticipantResponse update(
            @PathVariable UUID participantId,
            @Valid @RequestBody UpdateParticipantRequest request,
            Authentication authentication) {
        return ParticipantResponse.from(
                participantService.update(participantId, request.toCommand(authentication.getName())));
    }
}
