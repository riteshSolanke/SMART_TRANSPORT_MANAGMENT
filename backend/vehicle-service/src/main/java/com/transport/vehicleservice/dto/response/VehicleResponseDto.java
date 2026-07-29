package com.transport.vehicleservice.dto.response;

import com.transport.vehicleservice.enums.VehicleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VehicleResponseDto {
    private Long vehicleId;
    private String vehicleNumber;
    private Integer capacity;
    private VehicleStatus status;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
