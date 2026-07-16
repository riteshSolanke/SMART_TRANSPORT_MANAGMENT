package com.transport.authservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequestDto {
    @NotNull(message = "Active status is required")
    private Boolean active;
}