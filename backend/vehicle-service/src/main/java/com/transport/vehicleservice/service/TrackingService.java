package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.LocationRequestDto;
import com.transport.vehicleservice.dto.response.LocationResponseDto;

import java.util.List;

public interface TrackingService {
    LocationResponseDto record(Long vehicleId, LocationRequestDto request);
    LocationResponseDto latest(Long vehicleId);
    List<LocationResponseDto> history(Long vehicleId, int limit);
}
