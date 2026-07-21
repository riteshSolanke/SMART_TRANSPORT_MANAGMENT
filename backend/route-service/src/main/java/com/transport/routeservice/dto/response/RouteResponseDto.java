package com.transport.routeservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
@Getter
@Builder
@AllArgsConstructor
public class RouteResponseDto {
    private Long routeId;
    private String routeName;
    private String startPoint;
    private String endPoint;
    private boolean active;
    private List<StopResponseDto> stops;
    private List<ScheduleResponseDto> schedules;
}