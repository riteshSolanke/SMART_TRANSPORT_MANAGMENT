package com.transport.authservice.exception;

public class OtpRateLimitException extends RuntimeException {
    public OtpRateLimitException(String message) {
        super(message);
    }
}
