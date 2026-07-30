package com.transport.analyticsservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleAnalyticsDto {
    private Long scheduleId;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String daysOfWeek;
    private boolean active;
}
