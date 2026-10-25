package com.syncevent.events.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class EventSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/events")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/status")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/cancellation")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/postponements")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/postponements/confirmation")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/events")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR", "INVITADO")
                        .requestMatchers(HttpMethod.POST, "/api/participants")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/participants/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/participants", "/api/participants/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR")
                        .anyRequest().authenticated())
                .build();
    }
}