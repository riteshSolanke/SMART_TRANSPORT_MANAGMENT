package com.transport.analyticsservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RouteUsageAnalyticsDto {
    private Long routeId;
    private long ticketCount;
    private long passengerCount;
    private BigDecimal fareValue;
}
