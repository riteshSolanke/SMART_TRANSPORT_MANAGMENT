package com.transport.vehicleservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {
    @Bean
    RequestInterceptor routeRequestInterceptor(
            @Value("${gateway.secret-key}") String gatewaySecret) {
        return template -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException(
                        "Authenticated identity is required for route validation");
            }
            String role = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring(5))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Authenticated role is required for route validation"));
            template.header("X-User-Id", authentication.getName());
            template.header("X-User-Role", role);
            template.header("X-Gateway-Key", gatewaySecret);
        };
    }
}
