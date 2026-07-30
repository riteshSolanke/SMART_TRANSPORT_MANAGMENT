package com.transport.analyticsservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VehicleAnalyticsDto {
    private LocalDate from;
    private LocalDate to;
    private long totalAssignments;
    private long activeAssignments;
    private long completedAssignments;
    private long cancelledAssignments;
    private List<CompletedAssignmentAnalyticsDto> completedOperations;
}
