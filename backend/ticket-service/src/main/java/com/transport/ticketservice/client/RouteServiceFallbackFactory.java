package com.transport.ticketservice.client;

import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.FareResponseDto;
import com.transport.ticketservice.exception.RouteServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class RouteServiceFallbackFactory implements FallbackFactory<RouteServiceClient> {
    @Override
    public RouteServiceClient create(Throwable cause) {
        return new RouteServiceClient() {
            @Override
            public ApiResponseDto<FareResponseDto> getFare(
                    Long routeId, Long sourceStopId, Long destinationStopId,
                    Long scheduleId, String userId, String role) {
                log.error("Route Service call failed for routeId={}, scheduleId={}. Cause: {}",
                        routeId, scheduleId, cause.getClass().getSimpleName());
                throw new RouteServiceUnavailableException("Route Service is currently unavailable. Please try again later.", cause);
            }
        };
    }
}
