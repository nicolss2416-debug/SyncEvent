package com.syncevent.certificates.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = { "registration", "registration.event", "registration.participant" })
    List<CertificateEntity> findAll();

    @Override
    @EntityGraph(attributePaths = { "registration", "registration.event", "registration.participant" })
    Optional<CertificateEntity> findById(UUID id);

    @EntityGraph(attributePaths = { "registration", "registration.event", "registration.participant" })
    Optional<CertificateEntity> findByRegistrationId(UUID registrationId);

    @EntityGraph(attributePaths = { "registration", "registration.event", "registration.participant" })
    @Query("SELECT c FROM CertificateEntity c WHERE c.registration.participant.id = :participantId ORDER BY c.issuedAt DESC")
    List<CertificateEntity> findAllByParticipantIdOrderByIssuedAtDesc(@Param("participantId") UUID participantId);

    @EntityGraph(attributePaths = { "registration", "registration.event", "registration.participant" })
    Optional<CertificateEntity> findByVerificationCode(String verificationCode);
}
