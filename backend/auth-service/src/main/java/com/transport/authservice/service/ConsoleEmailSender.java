package com.transport.authservice.service;

	
	
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


