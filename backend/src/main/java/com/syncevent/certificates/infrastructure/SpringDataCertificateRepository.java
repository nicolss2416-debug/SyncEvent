package com.syncevent.certificates.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    Optional<CertificateEntity> findByRegistrationId(UUID registrationId);

    List<CertificateEntity> findAllByParticipantIdOrderByIssuedAtDesc(UUID participantId);

    Optional<CertificateEntity> findByVerificationCode(String verificationCode);
}
