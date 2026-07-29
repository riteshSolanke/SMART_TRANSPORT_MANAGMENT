package com.transport.routeservice.security;


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
public class InternalValidationFilter
        extends OncePerRequestFilter {

    private final byte[] secretKey;

    public InternalValidationFilter(@Value("${gateway.secret-key}") String secretKey) {
        if (secretKey == null || secretKey.length() < 32) {
            throw new IllegalStateException(
                    "GATEWAY_SHARED_SECRET must contain at least 32 characters");
        }
        this.secretKey = secretKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain filterChain)
            throws IOException, ServletException {

        String gatewayKey =
                req.getHeader("X-Gateway-Key");

        if (gatewayKey == null || !MessageDigest.isEqual(
                secretKey, gatewayKey.getBytes(StandardCharsets.UTF_8))) {
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Access only through API Gateway");
            return;
        }

        filterChain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/actuator/health".equals(request.getRequestURI())
                || "/actuator/info".equals(request.getRequestURI());
    }
}

