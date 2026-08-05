package com.transport.ticketservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class SeatAvailabilityResponseDto {
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private boolean assigned;
    private Long assignmentId;
    private Long vehicleId;
    private Integer capacity;
    private Long reservedSeats;
    private Long remainingSeats;
    private boolean available;
}
