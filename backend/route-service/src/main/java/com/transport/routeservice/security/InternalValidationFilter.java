package com.transport.routeservice.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class InternalValidationFilter
        extends OncePerRequestFilter {

    @Value("${gateway.secret-key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain filterChain)
            throws IOException, ServletException {

        String gatewayKey =
                req.getHeader("X-Gateway-Key");

        if(!secretKey.equals(gatewayKey)) {
            System.out.println("Gateway Key Validation Failed");
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Access only through API Gateway");
            return;
        }
        System.out.println("Gateway Key Validation Success");

        filterChain.doFilter(req, res);
    }
}

