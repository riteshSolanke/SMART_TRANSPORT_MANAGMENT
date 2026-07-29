package com.transport.ticketservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor requestInterceptor(
            @Value("${gateway.secret-key}") String gatewaySecret) {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }
            String role = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring(5))
                    .findFirst()
                    .orElse(null);
            template.header("X-User-Id", authentication.getName());
            if (role != null) {
                template.header("X-User-Role", role);
            }
            template.header("X-Gateway-Key", gatewaySecret);
        };
    }
}
