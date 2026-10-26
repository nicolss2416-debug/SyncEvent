package com.syncevent.users.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncevent.users.application.AdminUserService;

@Configuration
public class BootstrapAdministratorConfiguration {

    @Bean
    ApplicationRunner bootstrapAdministrator(
            AdminUserService adminUserService,
            @Value("${app.auth.bootstrap-admin.email:}") String email,
            @Value("${app.auth.bootstrap-admin.password:}") String password) {
        return arguments -> {
            boolean hasEmail = !email.isBlank();
            boolean hasPassword = !password.isBlank();
            if (hasEmail != hasPassword) {
                throw new IllegalStateException(
                        "Configure ambas variables BOOTSTRAP_ADMIN_EMAIL y BOOTSTRAP_ADMIN_PASSWORD, o ninguna.");
            }
            if (hasEmail) {
                adminUserService.provisionBootstrapAdministrator(email, password);
            }
        };
    }
}
