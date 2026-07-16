package com.transport.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailOtpRequestDto {

    @NotBlank(message = "OTP is required")
    private String otp;
}