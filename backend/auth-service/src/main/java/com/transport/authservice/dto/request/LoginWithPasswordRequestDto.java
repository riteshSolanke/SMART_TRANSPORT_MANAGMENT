package com.transport.authservice.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginWithPasswordRequestDto {

    @NotBlank(message= "Email is required")
    @Email(message = "Enter valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

}
