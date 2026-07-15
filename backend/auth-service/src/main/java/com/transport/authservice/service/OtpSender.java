package com.transport.authservice.service;

public interface OtpSender {

    void send(String mobileNumber, String otp);

}
