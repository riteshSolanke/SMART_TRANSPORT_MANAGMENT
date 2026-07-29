package com.transport.vehicleservice.service.impl;

import com.transport.vehicleservice.dto.request.LocationRequestDto;
import com.transport.vehicleservice.dto.response.LocationResponseDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.entity.VehicleLocation;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.LocationNotFoundException;
import com.transport.vehicleservice.exception.VehicleNotFoundException;
import com.transport.vehicleservice.mapper.VehicleMapper;
import com.transport.vehicleservice.repository.VehicleLocationRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {
    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;

    @Override
    @Transactional
    public LocationResponseDto record(Long vehicleId, LocationRequestDto request) {
        Vehicle vehicle = requireVehicle(vehicleId);
        if (vehicle.getStatus() != VehicleStatus.IN_SERVICE) {
            throw new IllegalArgumentException(
                    "Location updates require vehicle status IN_SERVICE");
        }
        LocalDateTime recordedAt =
                request.getRecordedAt() == null ? LocalDateTime.now() : request.getRecordedAt();
        if (recordedAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException(
                    "Location timestamp cannot be more than five minutes in the future");
        }

        VehicleLocation location = new VehicleLocation();
        location.setVehicle(vehicle);
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setSpeedKph(request.getSpeedKph());
        location.setRecordedAt(recordedAt);
        return VehicleMapper.toLocationDto(locationRepository.save(location));
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponseDto latest(Long vehicleId) {
        requireVehicle(vehicleId);
        return locationRepository
                .findFirstByVehicle_VehicleIdOrderByRecordedAtDesc(vehicleId)
                .map(VehicleMapper::toLocationDto)
                .orElseThrow(() -> new LocationNotFoundException(
                        "No location has been recorded for vehicle " + vehicleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponseDto> history(Long vehicleId, int limit) {
        requireVehicle(vehicleId);
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Location history limit must be 1 to 200");
        }
        return locationRepository
                .findByVehicle_VehicleIdOrderByRecordedAtDesc(
                        vehicleId, PageRequest.of(0, limit))
                .stream()
                .map(VehicleMapper::toLocationDto)
                .toList();
    }

    private Vehicle requireVehicle(Long vehicleId) {
        return vehicleRepository.findByVehicleIdAndActiveTrue(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Active vehicle not found with id: " + vehicleId));
    }
}
