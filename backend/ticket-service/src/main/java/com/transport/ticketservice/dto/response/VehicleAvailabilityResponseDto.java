package com.transport.ticketservice.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class VehicleAvailabilityResponseDto {
    private boolean assigned;
    private Long assignmentId;
    private Long vehicleId;
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private Integer capacity;
}
