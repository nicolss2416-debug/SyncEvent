package com.syncevent.events.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncevent.audit.api.AuditRequestFilter;
import com.syncevent.audit.application.AuditLogService;

@Configuration
@EnableMethodSecurity
public class EventSecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuditLogService auditLogService,
            ObjectMapper objectMapper,
            org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**", "/graphiql", "/graphiql/**", "/vendor/**").permitAll()
                        .requestMatchers("/graphql", "/graphql/**").authenticated()
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/register/**",
                                "/api/auth/login", "/api/auth/login/**",
                                "/api/api/auth/register", "/api/api/auth/register/**",
                                "/api/api/auth/login", "/api/api/auth/login/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/profile").hasRole("INVITADO")
                        .requestMatchers("/api/admin/audit", "/api/admin/audit/**")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/admin/users", "/api/admin/users/**")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/admin/certificates", "/api/admin/certificates/**")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers("/api/admin/attendance-policy", "/api/admin/attendance-policy/**", "/api/admin/fines", "/api/admin/fines/**")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/registrations/self")
                        .hasRole("INVITADO")
                        .requestMatchers(HttpMethod.GET, "/api/events/*/registrations/mine")
                        .hasRole("INVITADO")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/registrations/*/cancellation")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "INVITADO")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/registrations")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers("/api/events/*/attendance/**")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/events/*/status")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers("/api/events/*/monitors/**", "/api/events/*/monitors")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/cancellation")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/postponements")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/postponements/confirmation")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/participants/*/history")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/participants/me")
                        .hasRole("INVITADO")
                        .requestMatchers(HttpMethod.GET, "/api/events")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR", "INVITADO")
                        .requestMatchers("/api/public/certificates/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/registrations/*/certificate")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/certificates/**")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR", "INVITADO")
                        .requestMatchers(HttpMethod.POST, "/api/certificates/**")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/participants")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/participants/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                        .requestMatchers(HttpMethod.GET, "/api/participants", "/api/participants/*")
                        .hasAnyRole("ADMINISTRADOR", "OPERADOR", "MONITOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterAfter(new AuditRequestFilter(auditLogService, objectMapper),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    org.springframework.security.oauth2.server.resource.web.BearerTokenResolver bearerTokenResolver() {
        org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver resolver =
                new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver();
        return request -> {
            String path = request.getRequestURI();
            if (path.startsWith("/api/auth/register") || path.startsWith("/api/auth/login") || path.startsWith("/api/public/")
                    || path.startsWith("/api/api/auth/register") || path.startsWith("/api/api/auth/login") || path.startsWith("/api/api/public/")) {
                return null;
            }
            return resolver.resolve(request);
        };
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://localhost:5173,http://127.0.0.1:5173}") String allowedOriginPatterns) {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}