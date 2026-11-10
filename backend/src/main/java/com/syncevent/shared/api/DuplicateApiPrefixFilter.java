package com.syncevent.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that automatically fixes duplicate '/api/api/...' prefixes from clients
 * configured with a base URL that already included '/api'.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DuplicateApiPrefixFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/api/")) {
            String correctedUri = uri.substring(4); // Remove the first "/api"
            request.getRequestDispatcher(correctedUri).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
