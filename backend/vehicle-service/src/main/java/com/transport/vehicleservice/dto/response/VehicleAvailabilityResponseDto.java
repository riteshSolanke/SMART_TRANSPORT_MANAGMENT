package com.transport.vehicleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class VehicleAvailabilityResponseDto {
    private boolean assigned;
    private Long assignmentId;
    private Long vehicleId;
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private Integer capacity;
}
