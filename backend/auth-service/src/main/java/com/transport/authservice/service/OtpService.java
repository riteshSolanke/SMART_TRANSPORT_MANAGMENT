package com.transport.authservice.service;


import com.transport.authservice.entity.OtpVerification;
import com.transport.authservice.enums.OtpPurpose;
import com.transport.authservice.exception.InvalidTokenException;
import com.transport.authservice.exception.OtpAttemptExceededException;
import com.transport.authservice.exception.OtpRateLimitException;
import com.transport.authservice.repository.OtpVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;


@Slf4j
@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final OtpSender otpSender;
    private final EmailSender emailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int otpExpiryMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;

    private static final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpVerificationRepository otpRepository,
            OtpSender otpSender,
            EmailSender emailSender,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${otp.expiry-minutes}") int otpExpiryMinutes,
            @Value("${otp.max-attempts}") int maxAttempts,
            @Value("${otp.resend-cooldown-seconds}") int resendCooldownSeconds) {
        if (otpExpiryMinutes <= 0 || maxAttempts <= 0 || resendCooldownSeconds < 0) {
            throw new IllegalStateException("OTP configuration values are invalid");
        }
        this.otpRepository = otpRepository;
        this.otpSender = otpSender;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.otpExpiryMinutes = otpExpiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    private String generateSixDigitOtp(){
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    public void generateAndSendOtp(String identifier, OtpPurpose purpose) {
        otpRepository.findTopByIdentifierAndPurposeOrderByCreatedAtDesc(identifier, purpose)
                .filter(record -> record.getCreatedAt() != null)
                .filter(record -> record.getCreatedAt().isAfter(
                        LocalDateTime.now().minusSeconds(resendCooldownSeconds)))
                .ifPresent(record -> {
                    throw new OtpRateLimitException("otp.resend.too.soon");
                });

        String otp = generateSixDigitOtp();
        otpRepository.deleteByIdentifierAndPurpose(identifier, purpose);

        OtpVerification record = OtpVerification.builder()
                .identifier(identifier)
                .otpCode(passwordEncoder.encode(otp))
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();

        otpRepository.save(record);

        if (purpose == OtpPurpose.EMAIL_VERIFICATION) {
            emailSender.sendOtp(identifier, otp);
        } else {
            otpSender.send(identifier, otp);
        }

        log.info("OTP generated for identifier ending in {} with purpose {}",
                maskIdentifier(identifier), purpose);
    }




    @Transactional
    public void verifyOtp(String identifier, String otpInput, OtpPurpose purpose){

        OtpVerification record = otpRepository.findTopByIdentifierAndPurposeAndUsedFalseOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new InvalidTokenException("otp.expired"));

        if(record.getExpiresAt().isBefore(LocalDateTime.now())){
            record.setUsed(true);
            otpRepository.save(record);
            throw new InvalidTokenException("otp.expired");
        }

        if(record.getAttemptCount() >= maxAttempts){
            throw new OtpAttemptExceededException("otp.max.attempts");
        }

        if(!passwordEncoder.matches(otpInput, record.getOtpCode())){
            record.setAttemptCount(record.getAttemptCount()+ 1);
            otpRepository.save(record);
            if (record.getAttemptCount() >= maxAttempts) {
                throw new OtpAttemptExceededException("otp.max.attempts");
            }
            throw new InvalidTokenException("otp.invalid");
        }



        record.setUsed(true);
        otpRepository.save(record);

        log.info("OTP verified successfully for identifier ending in {} with purpose {}",
                maskIdentifier(identifier), purpose);

    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return identifier.substring(identifier.length() - 4);
    }

}




