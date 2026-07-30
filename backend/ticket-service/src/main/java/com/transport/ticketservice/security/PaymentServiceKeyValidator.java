package com.transport.ticketservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentServiceKeyValidator {
    private final byte[] expectedKey;

    public PaymentServiceKeyValidator(
            @Value("${payment.internal-secret}") String internalSecret) {
        if (internalSecret == null || internalSecret.length() < 32) {
            throw new IllegalStateException(
                    "PAYMENT_INTERNAL_SECRET must contain at least 32 characters");
        }
        expectedKey = internalSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void validate(String suppliedKey) {
        if (suppliedKey == null || !MessageDigest.isEqual(
                expectedKey, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("Invalid payment service identity");
        }
    }
}
