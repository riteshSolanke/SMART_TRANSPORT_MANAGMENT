package com.transport.authservice.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class ConsoleOtpSender implements OtpSender{

    @Value("${app.sms.mode}")
    private String smsMode;

    @Override
    public void send(String mobileNumber, String otp){
        log.info("-------------------------------------------------------");
        log.info("[DEV MODE - {} ] OTP for {} is: {}",  smsMode.toUpperCase(), mobileNumber, otp );
        log.info("===========================================================");
    }






}
