package com.syncevent.certificates.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.syncevent.certificates.domain.Certificate;
import com.syncevent.certificates.domain.CertificatePersistencePort;

@Component
public class CertificatePersistenceAdapter implements CertificatePersistencePort {

    private final SpringDataCertificateRepository repository;

    public CertificatePersistenceAdapter(SpringDataCertificateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Certificate save(Certificate certificate) {
        return repository.save(CertificateEntity.fromDomain(certificate)).toDomain();
    }

    @Override
    public Optional<Certificate> findById(UUID certificateId) {
        return repository.findById(certificateId).map(CertificateEntity::toDomain);
    }

    @Override
    public Optional<Certificate> findByRegistrationId(UUID registrationId) {
        return repository.findByRegistrationId(registrationId).map(CertificateEntity::toDomain);
    }

    @Override
    public Optional<Certificate> findByVerificationCode(String verificationCode) {
        return repository.findByVerificationCode(verificationCode).map(CertificateEntity::toDomain);
    }
}
