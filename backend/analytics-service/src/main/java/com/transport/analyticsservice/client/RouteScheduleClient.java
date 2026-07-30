package com.transport.analyticsservice.client;

import com.transport.analyticsservice.config.FeignConfig;
import com.transport.analyticsservice.dto.ApiResponseDto;
import com.transport.analyticsservice.dto.ScheduleAnalyticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "route-service", configuration = FeignConfig.class)
public interface RouteScheduleClient {
    @GetMapping("/api/routes/{routeId}/schedules/{scheduleId}")
    ApiResponseDto<ScheduleAnalyticsDto> getSchedule(
            @PathVariable("routeId") Long routeId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role);
}
