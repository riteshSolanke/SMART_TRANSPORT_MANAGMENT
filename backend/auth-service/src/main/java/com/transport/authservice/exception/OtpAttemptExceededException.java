package com.transport.authservice.exception;

public class OtpAttemptExceededException extends RuntimeException {
    public OtpAttemptExceededException(String message) {
        super(message);
    }
}
