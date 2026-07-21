package com.transport.routeservice.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;
@Getter
@Builder
@AllArgsConstructor
public class RouteSearchResponseDto {
    private Long routeId;
    private String routeName;
    private String startPoint;
    private String endPoint;
    private BigDecimal totalDistanceKm;
    private BigDecimal estimatedFare;
    private List<ScheduleResponseDto> availableSchedules;
}