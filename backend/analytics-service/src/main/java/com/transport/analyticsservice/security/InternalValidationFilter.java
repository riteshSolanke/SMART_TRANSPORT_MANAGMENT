package com.transport.analyticsservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalValidationFilter extends OncePerRequestFilter {
    private final byte[] secret;

    public InternalValidationFilter(
            @Value("${gateway.secret-key}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "GATEWAY_SHARED_SECRET must contain at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        String supplied = request.getHeader("X-Gateway-Key");
        if (supplied == null || !MessageDigest.isEqual(
                secret, supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Access only through API Gateway");
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/actuator/health".equals(request.getRequestURI())
                || "/actuator/info".equals(request.getRequestURI());
    }
}
