package com.transport.authservice.dto.request;

import com.transport.auth.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequestDto {
    @NotNull(message = "Role is required")
    private Role role;
}