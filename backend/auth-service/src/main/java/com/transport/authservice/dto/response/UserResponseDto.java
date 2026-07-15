package com.transport.authservice.dto.response;


import com.transport.authservice.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {


    private Long userId;
    private String mobileNumber;
    private  String email;
    private  String name;
    private Role role;
    private boolean mobileVerified;
    private boolean emailVerified;

}
