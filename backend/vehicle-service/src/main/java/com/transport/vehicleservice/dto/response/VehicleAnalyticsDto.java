package com.transport.vehicleservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class VehicleAnalyticsDto {
    private LocalDate from;
    private LocalDate to;
    private long totalAssignments;
    private long activeAssignments;
    private long completedAssignments;
    private long cancelledAssignments;
    private List<CompletedAssignmentAnalyticsDto> completedOperations;
}
