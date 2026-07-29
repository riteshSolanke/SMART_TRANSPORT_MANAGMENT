package com.transport.ticketservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
