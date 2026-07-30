package com.transport.analyticsservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class PerformanceAnalyticsDto {
    private LocalDate from;
    private LocalDate to;
    private long totalAssignments;
    private long activeAssignments;
    private long completedAssignments;
    private long cancelledAssignments;
    private long onTimeAssignments;
    private BigDecimal onTimePerformance;
    private int graceMinutes;
}
