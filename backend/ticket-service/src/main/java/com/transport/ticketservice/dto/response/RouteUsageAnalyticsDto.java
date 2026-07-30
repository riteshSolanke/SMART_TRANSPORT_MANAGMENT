package com.transport.ticketservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RouteUsageAnalyticsDto {
    private Long routeId;
    private long ticketCount;
    private long passengerCount;
    private BigDecimal fareValue;
}
