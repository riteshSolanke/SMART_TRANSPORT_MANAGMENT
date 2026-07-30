package com.transport.analyticsservice.client;

import com.transport.analyticsservice.config.FeignConfig;
import com.transport.analyticsservice.dto.ApiResponseDto;
import com.transport.analyticsservice.dto.VehicleAnalyticsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "vehicle-service", configuration = FeignConfig.class)
public interface VehicleAnalyticsClient {
    @GetMapping("/api/vehicles/internal/analytics")
    ApiResponseDto<VehicleAnalyticsDto> summarize(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role);
}
