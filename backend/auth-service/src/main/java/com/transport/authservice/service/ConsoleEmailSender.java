package com.transport.authservice.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

    @Slf4j
	@Service
	public class ConsoleEmailSender implements EmailSender {
	    @Override
	    public void sendOtp(String email, String otp) {
	        log.info("=================================================");
	        log.info(" [DEV MODE] Email OTP for {} is: {}", email, otp);
	        log.info("=================================================");
	    }
	}


