package com.transport.ticketservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
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
            template.header("X-Gateway-Key", gatewaySecret);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Feign authentication = {}", authentication);

            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No thread-local authentication; using explicit Feign identity headers");
                return;
            }
            String role = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring(5))
                    .findFirst()
                    .orElse(null);
            if (!template.headers().containsKey("X-User-Id")) {
                template.header("X-User-Id", authentication.getName());
            }
            if (role != null && !template.headers().containsKey("X-User-Role")) {
                template.header("X-User-Role", role);
            }
        };
    }
}
