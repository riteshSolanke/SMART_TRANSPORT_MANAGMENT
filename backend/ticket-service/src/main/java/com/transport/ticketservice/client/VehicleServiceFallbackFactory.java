package com.transport.ticketservice.client;

import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.VehicleAvailabilityResponseDto;
import com.transport.ticketservice.exception.VehicleServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class VehicleServiceFallbackFactory
        implements FallbackFactory<VehicleServiceClient> {
    @Override
    public VehicleServiceClient create(Throwable cause) {
        return new VehicleServiceClient() {
            @Override
            public ApiResponseDto<VehicleAvailabilityResponseDto> getAvailability(
                    Long routeId, Long scheduleId, LocalDate serviceDate,
                    String userId, String role) {
                log.error(
                        "Vehicle Service call failed for routeId={}, scheduleId={}, serviceDate={}. Cause: {}",
                        routeId, scheduleId, serviceDate,
                        cause.getClass().getSimpleName());
                throw new VehicleServiceUnavailableException(
                        "Vehicle Service is currently unavailable. Please try again later.",
                        cause);
            }
        };
    }
}
