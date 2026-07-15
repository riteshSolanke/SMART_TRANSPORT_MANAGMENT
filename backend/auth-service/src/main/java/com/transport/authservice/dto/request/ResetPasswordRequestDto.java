package com.transport.authservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequestDto {

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;


    @NotBlank(message = "New password is required")
    @Size(min=5, message = "password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$", message = "Password must contain at least one uppercase,one lowercase, and one digit")
    private String newPassword;

}
