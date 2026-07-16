package com.transport.authservice.service;

public interface EmailSender {
    void sendOtp(String email, String otp);
}