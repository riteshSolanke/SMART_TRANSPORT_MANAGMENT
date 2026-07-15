package com.transport.authservice.service;


import com.transport.authservice.dto.request.CompleteRegistrationRequestDto;
import com.transport.authservice.dto.request.LoginWithPasswordRequestDto;
import com.transport.authservice.dto.request.ResetPasswordRequestDto;
import com.transport.authservice.dto.response.AuthResponseDto;
import com.transport.authservice.dto.response.UserResponseDto;
import com.transport.authservice.entity.RefreshToken;
import com.transport.authservice.entity.User;
import com.transport.authservice.enums.OtpPurpose;
import com.transport.authservice.enums.Role;
import com.transport.authservice.exception.DuplicateResourceException;
import com.transport.authservice.exception.InvalidCredentialsException;
import com.transport.authservice.exception.InvalidTokenException;
import com.transport.authservice.exception.ResourceNotFoundException;
import com.transport.authservice.repository.RefreshTokenRepository;
import com.transport.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;


// ============== Helper Methods ================
    private AuthResponseDto generateAuthResponse(User user){
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateAccessToken(user);

        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(tokenEntity);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toUserResponseDto(user))
                .build();
    }

    private UserResponseDto toUserResponseDto(User user){
        return UserResponseDto.builder()
                .userId(user.getUserId())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .mobileVerified(user.isMobileVerified())
                .emailVerified(user.isEmailVerified())
                .build();
    }



//    Registration flow...


//  1(flow).   Send Otp for Registration
    public void sendRegistrationOtp(String mobileNumber){

        if(userRepository.existsByMobileNumber(mobileNumber)){
            throw new DuplicateResourceException("mobile.already.registered");
        }

        otpService.generateAndSendOtp(mobileNumber, OtpPurpose.REGISTRATION);

    }


//  2(flow).  Verify Registration OTP
    public String verifyRegistrationOtp(String mobileNumber, String otp){
        //   Generate short-lived token for registration...
        otpService.verifyOtp(mobileNumber, otp, OtpPurpose.REGISTRATION);
        return jwtService.generateRegistrationToken(mobileNumber);
    }


//    3(flow). Complete Registration for User
    @Transactional
    public AuthResponseDto completeRegistration(String mobileNumber, CompleteRegistrationRequestDto dto){
        if(userRepository.existsByMobileNumber(mobileNumber)){
            throw new DuplicateResourceException("mobile.already.registered");
        }

        if(dto.getEmail() != null && !dto.getEmail().isBlank() && userRepository.existsByEmail(dto.getEmail())){
            throw new DuplicateResourceException("email.already.exists");
        }

        User user = User.builder()
                .mobileNumber(mobileNumber)
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(Role.PASSENGER)
                .mobileVerified(true)
                .emailVerified(false)
                .active(true)
                .preferredLanguage(dto.getPreferredLanguage() != null ? dto.getPreferredLanguage(): "en")
                .build();

        user = userRepository.save(user);

        log.info("New user registered: userId={}, mobile={}", user.getUserId(), mobileNumber);

        return generateAuthResponse(user);
    }


//    update lang
    @Transactional
    public void updatePreferredLanguage(Long userId, String language){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("mobile.not.found"));

        user.setPreferredLanguage(language);
        userRepository.save(user);

        log.info("Language preference updated for userID={} to {}", userId, language);

    }

//    ===================== Login flow ===================

//    Login using OTP (first priority)
    public void sendLoginOtp(String mobileNumber){
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(()-> new ResourceNotFoundException("mobile.not.found"));

        if(!user.isActive()){
            throw new ResourceNotFoundException("account.deactivated");
        }

        otpService.generateAndSendOtp(mobileNumber, OtpPurpose.LOGIN);
    }



    @Transactional
    public AuthResponseDto loginWithOtp(String mobileNumber, String otp){
        otpService.verifyOtp(mobileNumber, otp, OtpPurpose.LOGIN);

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(()-> new ResourceNotFoundException("user.not-found"));
        log.info("User logged in via OTP: userId = {}", user.getUserId());
        return generateAuthResponse(user);
    }


//    Verify OTP or Login with OTP
    @Transactional
    public AuthResponseDto loginWithPassword (LoginWithPasswordRequestDto dto){

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(()-> new InvalidCredentialsException("credentials.invalid"));

        if(user.getPasswordHash() == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())){
            throw new InvalidCredentialsException("credentials.invalid");
        }


        if(!user.isActive()){
            throw new ResourceNotFoundException("account.deactivated");
        }

        log.info("User logged in via email + password: userId = {}", user.getUserId());

        return generateAuthResponse(user);
    }



//    ================= Forgot password (OTP-based) ===============

    public void sendForgotPasswordOtp(String mobileNumber){
        userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(()-> new ResourceNotFoundException("mobile.not.found"));
        otpService.generateAndSendOtp(mobileNumber, OtpPurpose.RESET_PASSWORD);
    }


 public String verifyForgotPasswordOtp(String mobileNumber, String otp){
        otpService.verifyOtp(mobileNumber, otp, OtpPurpose.RESET_PASSWORD);
        return jwtService.generateResetToken(mobileNumber);
 }


 @Transactional
    public void resetPassword(String mobileNumber, ResetPasswordRequestDto dto){

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("user.not-found"));

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));

        userRepository.save(user);

        refreshTokenRepository.deleteByUserId(user.getUserId());

        log.info("Password reset successfully for userId={}", user.getUserId());
    }

//    ================= Token Refresh ===============

    @Transactional
    public AuthResponseDto refreshAccessToken(String refreshToken){
        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(()-> new InvalidTokenException("token.refresh.invalid"));

        if(storedToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new InvalidTokenException("token.refresh.expire");
        }
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(()-> new ResourceNotFoundException("user.not-found"));

        String newAccessToken = jwtService.generateAccessToken(user);
        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toUserResponseDto(user))
                .build();

    }



// ========================== Logout ====================

    @Transactional
    public void logout(Long userId){
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User logged out: userId={}", userId);
    }


}
