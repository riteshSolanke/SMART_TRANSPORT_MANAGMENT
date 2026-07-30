package com.transport.ticketservice.client;

import com.transport.ticketservice.config.FeignConfig;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.VehicleAvailabilityResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(
        name = "vehicle-service",
        configuration = FeignConfig.class,
        fallbackFactory = VehicleServiceFallbackFactory.class)
public interface VehicleServiceClient {
    @GetMapping("/api/vehicles/assignments/availability")
    ApiResponseDto<VehicleAvailabilityResponseDto> getAvailability(
            @RequestParam("routeId") Long routeId,
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("serviceDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate serviceDate);
}
