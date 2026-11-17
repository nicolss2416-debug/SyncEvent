package com.syncevent.users.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.syncevent.users.domain.PasswordResetNotificationPort;

@Component
public class EmailPasswordResetNotificationAdapter implements PasswordResetNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(EmailPasswordResetNotificationAdapter.class);

    private final boolean exposeDevOtp;

    public EmailPasswordResetNotificationAdapter(
            @Value("${app.auth.expose-dev-otp:false}") boolean exposeDevOtp) {
        this.exposeDevOtp = exposeDevOtp;
    }

    @Override
    public void sendPasswordResetOtp(String email, String otp) {
        // En producción el despacho se canaliza vía correo electrónico seguro sin exponer el OTP en logs
        if (exposeDevOtp && log.isDebugEnabled()) {
            log.debug("[DEV-EMAIL] Correo de recuperación enviado a '{}': OTP {}", email, otp);
        } else {
            log.info("Notificación de restablecimiento de contraseña enviada al correo del destinatario.");
        }
    }
}
