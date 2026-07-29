package com.transport.ticketservice.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {

            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs == null) return;
                HttpServletRequest request = attrs.getRequest();
                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                String gatewayKey = request.getHeader("X-Gateway-Key");
                if (userId != null) template.header("X-User-Id", userId);
                if (role != null) template.header("X-User-Role", role);
                if (gatewayKey != null) {
                    template.header("X-Gateway-Key", gatewayKey);
                }
            }
        };
    }
}
