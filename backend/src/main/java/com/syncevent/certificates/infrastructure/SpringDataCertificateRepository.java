package com.syncevent.certificates.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    Optional<CertificateEntity> findByRegistrationId(UUID registrationId);

    Optional<CertificateEntity> findByVerificationCode(String verificationCode);
}
