package com.transport.routeservice.security;

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
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "PASSENGER", "CONDUCTOR", "DISPATCHER", "TRANSPORT_MANAGER", "ADMIN");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (userId != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (!isValidUserId(userId) || !ALLOWED_ROLES.contains(role)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway identity");
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isValidUserId(String userId) {
        try {
            return Long.parseLong(userId) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
