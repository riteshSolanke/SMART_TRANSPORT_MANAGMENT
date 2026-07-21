package com.transport.routeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalTime;
@Getter
@Builder
@AllArgsConstructor
public class ScheduleResponseDto {
    private Long scheduleId;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String daysOfWeek;
    private boolean active;
}
