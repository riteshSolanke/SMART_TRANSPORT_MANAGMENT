package com.transport.vehicleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CompletedAssignmentAnalyticsDto {
    private Long assignmentId;
    private Long routeId;
    private Long scheduleId;
    private LocalDate serviceDate;
    private LocalDateTime completedAt;
}
