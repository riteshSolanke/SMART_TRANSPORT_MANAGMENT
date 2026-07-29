package com.transport.vehicleservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "PASSENGER", "CONDUCTOR", "DISPATCHER", "TRANSPORT_MANAGER", "ADMIN");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (userId != null && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!validUserId(userId) || !ALLOWED_ROLES.contains(role)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid gateway identity");
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        }
        filterChain.doFilter(request, response);
    }

    private boolean validUserId(String userId) {
        try {
            return Long.parseLong(userId) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
