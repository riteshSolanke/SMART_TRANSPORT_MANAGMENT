package com.transport.routeservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResponseDto {
    private Long routeId;
    private Long sourceStopId;
    private Long destinationStopId;
    private Long scheduleId;
    private LocalTime departureTime;
    private BigDecimal distanceKm;
    private BigDecimal fare;
    private boolean peakHourApplied;
}
