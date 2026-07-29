package com.transport.routeservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 30, message = "Days of week cannot exceed 30 characters")
    private String daysOfWeek;
}
