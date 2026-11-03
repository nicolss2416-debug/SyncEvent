package com.syncevent.certificates.api;

import java.util.UUID;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncevent.certificates.application.CertificateIssueService;
import com.syncevent.certificates.application.CertificateVerificationService;
import com.syncevent.certificates.domain.Certificate;
import com.syncevent.users.domain.UserAccount;
import com.syncevent.users.domain.UserPersistencePort;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Validated
public class CertificateController {

    private final CertificateIssueService certificateIssueService;
    private final CertificateVerificationService certificateVerificationService;
    private final UserPersistencePort userPersistencePort;

    public CertificateController(
            CertificateIssueService certificateIssueService,
            CertificateVerificationService certificateVerificationService,
            UserPersistencePort userPersistencePort) {
        this.certificateIssueService = certificateIssueService;
        this.certificateVerificationService = certificateVerificationService;
        this.userPersistencePort = userPersistencePort;
    }

    @PostMapping("/events/{eventId}/registrations/{registrationId}/certificate")
    public ResponseEntity<CertificateResponse> issue(
            @PathVariable UUID eventId,
            @PathVariable UUID registrationId,
            Authentication authentication) {
        UUID actorId = userPersistencePort.findByEmail(authentication.getName())
                .map(UserAccount::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Certificate certificate = certificateIssueService.issueForEvent(
            eventId, registrationId, actorId, authentication.getName());
        return ResponseEntity.ok(CertificateResponse.from(certificate));
    }

    @GetMapping("/certificates/{certificateId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID certificateId, Authentication authentication) {
        UUID participantId = participantIdFor(authentication);
        Certificate certificate = certificateIssueService.findByIdForParticipant(certificateId, participantId);
        byte[] pdf = certificateIssueService.download(certificateId, participantId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=certificate-%s.pdf".formatted(certificate.verificationCode()))
                .body(pdf);
    }

    @GetMapping("/certificates/mine")
    public List<CertificateResponse> myCertificates(Authentication authentication) {
        UUID participantId = participantIdFor(authentication);
        return certificateIssueService.findAllForParticipant(participantId).stream()
                .map(CertificateResponse::from)
                .toList();
    }

    @GetMapping("/admin/certificates")
    public List<CertificateResponse> allCertificates() {
        return certificateIssueService.findAll().stream().map(CertificateResponse::from).toList();
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    public ResponseEntity<CertificateResponse> revoke(
            @PathVariable UUID certificateId,
            @Valid @RequestBody RevokeCertificateRequest request,
            Authentication authentication) {
        UUID actorId = userPersistencePort.findByEmail(authentication.getName())
                .map(UserAccount::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Certificate certificate = certificateIssueService.revoke(certificateId, request.reason(), actorId, authentication.getName());
        return ResponseEntity.ok(CertificateResponse.from(certificate));
    }

    @GetMapping("/public/certificates/{verificationCode}")
    public CertificateVerificationResponse verifyPublic(@PathVariable String verificationCode) {
        return CertificateVerificationResponse.from(certificateVerificationService.verify(verificationCode));
    }

    @GetMapping("/public/certificates/verify/{verificationCode}")
    public CertificateVerificationResponse verifyPublicAlias(@PathVariable String verificationCode) {
        return verifyPublic(verificationCode);
    }

    private UUID participantIdFor(Authentication authentication) {
        return userPersistencePort.findByEmail(authentication.getName())
                .map(UserAccount::getParticipantId)
                .orElseThrow(() -> new AccessDeniedException("No se encontró la cuenta autenticada."));
    }
}
