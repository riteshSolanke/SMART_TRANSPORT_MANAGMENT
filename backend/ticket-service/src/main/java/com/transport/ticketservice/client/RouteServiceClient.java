package com.transport.ticketservice.client;

import com.transport.ticketservice.config.FeignConfig;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "route-service",
        configuration = FeignConfig.class,
        fallbackFactory = RouteServiceFallbackFactory.class
)
public interface RouteServiceClient {

    @GetMapping("/api/routes/{routeId}/fare")
    ApiResponseDto<FareResponseDto> getFare(
            @PathVariable("routeId") Long routeId,
            @RequestParam("sourceStopId") Long sourceStopId,
            @RequestParam("destinationStopId") Long destinationStopId,
            @RequestParam("scheduleId") Long scheduleId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role
    );
}
