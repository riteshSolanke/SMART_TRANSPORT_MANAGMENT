package com.transport.authservice.service;


import com.transport.authservice.entity.OtpVerification;
import com.transport.authservice.enums.OtpPurpose;
import com.transport.authservice.exception.InvalidTokenException;
import com.transport.authservice.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final OtpSender otpSender;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final SecureRandom secureRandom = new SecureRandom();

    private String generateSixDigitOtp(){
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    public void generateAndSendOtp(String mobileNumber, OtpPurpose purpose){
        String otp = generateSixDigitOtp();

        OtpVerification record = OtpVerification.builder()
                .identifier(mobileNumber)
                .otpCode(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        otpRepository.save(record);
        otpSender.send(mobileNumber, otp);

        log.info("OTP generated for {} with purpose {}", mobileNumber, purpose);

    }


    public void verifyOtp(String mobileNumber, String otpInput, OtpPurpose purpose){

       OtpVerification record = otpRepository.findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(mobileNumber, purpose)
               .orElseThrow(() -> new InvalidTokenException("otp.expired"));

       if(record.getAttemptCount() >= MAX_ATTEMPTS){
           throw new RuntimeException("otp.max.attempts");
       }

       if(!passwordEncoder.matches(otpInput, record.getOtpCode())){
           record.setAttemptCount(record.getAttemptCount()+ 1);
           otpRepository.save(record);
           throw new InvalidTokenException("otp.invalid");
       }

       record.setUsed(true);
       otpRepository.save(record);

       log.info("OTP verified successfully for {} with purpose {}", mobileNumber, purpose);


    }

}
