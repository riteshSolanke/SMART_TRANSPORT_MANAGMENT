package com.transport.ticketservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component

public class InternalValidationFilter extends OncePerRequestFilter {

    @Value("${gateway.secret-key}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws IOException, ServletException {

        String gatewayKey = req.getHeader("X-Gateway-Key");

        if(!secretKey.equals(gatewayKey)){
            res.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Access only thorugh API Gateway"
            );
            return;
        }

        filterChain.doFilter(req, res);

    }

}
