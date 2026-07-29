package com.transport.vehicleservice.client;

import com.transport.vehicleservice.config.FeignConfig;
import com.transport.vehicleservice.dto.response.ApiResponseDto;
import com.transport.vehicleservice.dto.response.RouteReferenceDto;
import com.transport.vehicleservice.dto.response.ScheduleReferenceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "route-service", configuration = FeignConfig.class)
public interface RouteServiceClient {
    @GetMapping("/api/routes/{routeId}")
    ApiResponseDto<RouteReferenceDto> getRoute(@PathVariable("routeId") Long routeId);

    @GetMapping("/api/routes/{routeId}/schedules/{scheduleId}")
    ApiResponseDto<ScheduleReferenceDto> getSchedule(
            @PathVariable("routeId") Long routeId,
            @PathVariable("scheduleId") Long scheduleId);
}
