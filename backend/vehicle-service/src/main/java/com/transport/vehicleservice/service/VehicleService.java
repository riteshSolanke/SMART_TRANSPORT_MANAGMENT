package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.VehicleRequestDto;
import com.transport.vehicleservice.dto.response.VehicleResponseDto;
import com.transport.vehicleservice.enums.VehicleStatus;

import java.util.List;

public interface VehicleService {
    VehicleResponseDto create(VehicleRequestDto request);
    List<VehicleResponseDto> getAll();
    VehicleResponseDto get(Long vehicleId);
    VehicleResponseDto update(Long vehicleId, VehicleRequestDto request);
    VehicleResponseDto updateStatus(Long vehicleId, VehicleStatus status);
    void delete(Long vehicleId);
}
