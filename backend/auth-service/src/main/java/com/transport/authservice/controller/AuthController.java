package com.transport.authservice.controller;


import com.transport.authservice.dto.request.*;
import com.transport.authservice.dto.response.ApiResponseDto;
import com.transport.authservice.dto.response.AuthResponseDto;
import com.transport.authservice.service.AuthService;
import com.transport.authservice.service.JwtService;
import com.transport.authservice.util.MessageUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final MessageUtil messageUtil;
    private final MessageSource messageSource;

//    ==================== REGISTRATION ====================

    @PostMapping("/register/send-otp")
    public ResponseEntity<ApiResponseDto<Void>> sendRegistrationOtp(@Valid @RequestBody SendOtpRequestDto dto){
        log.info("Registration OTP requested for mobile: {}", dto.getMobileNumber());

        authService.sendRegistrationOtp(dto.getMobileNumber());
         return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("otp.sent.success")));
    }


    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponseDto<String>> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequestDto dto){

        String registrationToken = authService.verifyRegistrationOtp(dto.getMobileNumber(), dto.getOtp());
        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("otp.verified.success"), registrationToken));
    }



    @PostMapping("/register/complete")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> completeRegistration(@RequestHeader("Authorization") String authHeader,  @Valid @RequestBody CompleteRegistrationRequestDto dto
                                                                                ){

        String mobileNumber = extractMobileFromToken(authHeader, "REGISTRATION");

        AuthResponseDto response = authService.completeRegistration(mobileNumber, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(messageUtil.getMessage("registration.success"), response));
    }



//    ===================== OTP LOGIN ================

    @PostMapping("/login/send-otp")
    public ResponseEntity<ApiResponseDto<Void>> sendLoginOtp(@Valid @RequestBody SendOtpRequestDto dto){

        authService.sendLoginOtp(dto.getMobileNumber());
        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("otp.sent.success")));

    }



   @PostMapping("/login/verify-otp")
   public ResponseEntity<ApiResponseDto<AuthResponseDto>> loginWithOtp(@Valid @RequestBody VerifyOtpRequestDto dto){

        AuthResponseDto response = authService.loginWithOtp(dto.getMobileNumber(), dto.getOtp());

        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("login.success"), response));

    }


//  FALLBACK Login (Email, password)
    @PostMapping("/login/password")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> loginWithPassword(@Valid @RequestBody LoginWithPasswordRequestDto dto){
        AuthResponseDto response = authService.loginWithPassword(dto);

        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("login.success"), response));
    }



// ======================= FORGOT PASSWORD ===============

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponseDto<Void>> sendForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequestDto dto){
       authService.sendForgotPasswordOtp(dto.getMobileNumber());
       return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("otp.sent.success")));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponseDto<String>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequestDto dto){

        String resetToken = authService.verifyForgotPasswordOtp(dto.getMobileNumber(), dto.getOtp());
        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("otp.verified.success"), resetToken));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponseDto<Void>> resetPassword(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody ResetPasswordRequestDto dto){
        String mobileNumber = extractMobileFromToken(authHeader, "RESET_PASSWORD");

        authService.resetPassword(mobileNumber, dto);

        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("password.reset.success")));
    }



//    =================== TOKEN REFRESH & LOGOUT ==============

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto dto){
        System.out.println("RefreshToken:- " + dto.getRefreshToken());
        AuthResponseDto response = authService.refreshAccessToken(dto.getRefreshToken());

        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("token.refresh"), response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
           Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        authService.logout(userId);

        return ResponseEntity.ok(
                ApiResponseDto.success(messageUtil.getMessage("logout.success")));
    }

    @PatchMapping("/preferences/language")
    public ResponseEntity<ApiResponseDto<Void>> updateLanguage(Authentication authentication, @Valid @RequestBody UpdateLanguageDto dto){
        Long userId = Long.parseLong(authentication.getName());
        authService.updatePreferredLanguage(userId, dto.getPreferredLanguage());

        return ResponseEntity.ok(ApiResponseDto.success(messageUtil.getMessage("language.update.success")));

    }


//================ HELPER =====================
    private String extractMobileFromToken(String authHeader, String expectedType) {

        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtService.extractClaims(token);
        String tokenType = claims.get("type", String.class);
        if(!expectedType.equals(tokenType)){
            throw new RuntimeException("token.invalid");
        }
       return claims.getSubject();
    }

}
