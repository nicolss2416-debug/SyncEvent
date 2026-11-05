package com.syncevent.certificates.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificatePersistencePort {

    Certificate save(Certificate certificate);

    List<Certificate> findAll();

    Optional<Certificate> findById(UUID certificateId);

    List<Certificate> findAllByParticipantId(UUID participantId);

    Optional<Certificate> findByRegistrationId(UUID registrationId);

    Optional<Certificate> findByVerificationCode(String verificationCode);
}
