package com.transport.routeservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;
@Getter
@Setter
public class ScheduleRequestDto {
    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;
    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;
    @NotBlank(message = "Days of week is required")
    private String daysOfWeek;
}