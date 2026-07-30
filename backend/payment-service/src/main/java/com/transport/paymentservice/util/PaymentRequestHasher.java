package com.transport.paymentservice.util;

import com.transport.paymentservice.dto.request.PaymentRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PaymentRequestHasher {
    public String hash(Long userId, PaymentRequestDto request) {
        String value = userId + "|" + request.getTicketId()
                + "|" + request.getPaymentMethod();
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
