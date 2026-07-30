package com.transport.analyticsservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CompletedAssignmentAnalyticsDto {
    private Long assignmentId;
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private LocalDateTime completedAt;
}
