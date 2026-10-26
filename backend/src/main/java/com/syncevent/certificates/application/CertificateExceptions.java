package com.syncevent.certificates.application;

public final class CertificateExceptions {

    private CertificateExceptions() {
    }

    public static class CertificateNotFoundException extends RuntimeException {
        public CertificateNotFoundException(String code) {
            super("No existe un certificado para el código %s.".formatted(code));
        }

        public CertificateNotFoundException(java.util.UUID certificateId) {
            super("No existe un certificado con identificador %s.".formatted(certificateId));
        }
    }

    public static class CertificateNotEligibleException extends RuntimeException {
        public CertificateNotEligibleException(String message) {
            super(message);
        }
    }

    public static class CertificateAlreadyRevokedException extends RuntimeException {
        public CertificateAlreadyRevokedException(String code) {
            super("El certificado %s ya fue revocado.".formatted(code));
        }
    }

    public static class CertificateRevocationNotAllowedException extends RuntimeException {
        public CertificateRevocationNotAllowedException(String message) {
            super(message);
        }
    }
}
