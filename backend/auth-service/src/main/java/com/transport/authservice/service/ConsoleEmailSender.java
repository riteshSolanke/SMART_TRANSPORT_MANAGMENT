package com.transport.authservice.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

    @Slf4j
	@Service
	public class ConsoleEmailSender implements EmailSender {
	    @Value("${app.otp.expose-in-logs:false}")
	    private boolean exposeOtpInLogs;

	    @Override
	    public void sendOtp(String email, String otp) {
	        if (exposeOtpInLogs) {
	            log.warn("[DEV MODE] Email OTP delivery for {}: {}", maskEmail(email), otp);
	            return;
	        }
	        log.info("[DEV MODE] Email OTP delivery requested for {}", maskEmail(email));
	    }

	    private String maskEmail(String email) {
	        if (email == null || !email.contains("@")) {
	            return "****";
	        }
	        return email.charAt(0) + "***" + email.substring(email.indexOf('@'));
	    }
	}


