package com.transport.vehicleservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequestDto {
    @NotBlank
    @Size(min = 2, max = 50)
    private String vehicleNumber;

    @NotNull
    @Min(1)
    @Max(500)
    private Integer capacity;
}
