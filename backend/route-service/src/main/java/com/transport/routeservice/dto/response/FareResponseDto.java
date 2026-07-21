package com.transport.routeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
@Getter
@Builder
@AllArgsConstructor
public class FareResponseDto {
    private Long routeId;
    private Long sourceStopId;
    private Long destinationStopId;
    private BigDecimal distanceKm;
    private BigDecimal fare;
    private boolean peakHourApplied;
}