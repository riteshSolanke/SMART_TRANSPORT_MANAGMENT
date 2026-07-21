package com.transport.authservice.dto.request;

import com.transport.authservice.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;


@Data
public class CreateStaffRequestDto {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message= "Enter a valid mobile number")
    private String mobileNumber;

    @NotBlank(message= "Name is required")
    private String name;

    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min=6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

}
