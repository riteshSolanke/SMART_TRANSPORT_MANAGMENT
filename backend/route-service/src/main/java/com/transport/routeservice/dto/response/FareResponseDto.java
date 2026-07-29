package com.transport.routeservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResponseDto {
    private Long routeId;
    private Long sourceStopId;
    private Long destinationStopId;
    private BigDecimal distanceKm;
    private BigDecimal fare;
    private boolean peakHourApplied;
}