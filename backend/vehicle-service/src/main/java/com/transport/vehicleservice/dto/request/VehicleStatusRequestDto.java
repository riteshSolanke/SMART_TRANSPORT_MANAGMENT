package com.transport.vehicleservice.dto.request;

import com.transport.vehicleservice.enums.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleStatusRequestDto {
    @NotNull
    private VehicleStatus status;
}
