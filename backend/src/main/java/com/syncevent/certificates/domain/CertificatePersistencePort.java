package com.syncevent.certificates.domain;

import java.util.Optional;
import java.util.UUID;

public interface CertificatePersistencePort {

    Certificate save(Certificate certificate);

    Optional<Certificate> findById(UUID certificateId);

    Optional<Certificate> findByRegistrationId(UUID registrationId);

    Optional<Certificate> findByVerificationCode(String verificationCode);
}
