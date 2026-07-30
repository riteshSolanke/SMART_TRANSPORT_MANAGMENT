package com.transport.paymentservice.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    @Bean
    RequestInterceptor ticketRequestInterceptor(
            @Value("${gateway.secret-key}") String gatewaySecret,
            @Value("${payment.internal-secret}") String paymentSecret) {
        validateSecret(gatewaySecret, "GATEWAY_SHARED_SECRET");
        validateSecret(paymentSecret, "PAYMENT_INTERNAL_SECRET");
        return template -> {
            template.header("X-Gateway-Key", gatewaySecret);
            template.header("X-Payment-Service-Key", paymentSecret);
        };
    }

    private void validateSecret(String secret, String name) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    name + " must contain at least 32 characters");
        }
    }
}
