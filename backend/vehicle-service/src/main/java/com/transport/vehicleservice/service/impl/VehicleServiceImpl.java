package com.transport.vehicleservice.service.impl;

import com.transport.vehicleservice.dto.request.VehicleRequestDto;
import com.transport.vehicleservice.dto.response.VehicleResponseDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.AssignmentConflictException;
import com.transport.vehicleservice.exception.DuplicateVehicleException;
import com.transport.vehicleservice.exception.VehicleNotFoundException;
import com.transport.vehicleservice.mapper.VehicleMapper;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    private final VehicleAssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public VehicleResponseDto create(VehicleRequestDto request) {
        String number = normalizeNumber(request.getVehicleNumber());
        if (vehicleRepository.existsByVehicleNumberIgnoreCase(number)) {
            throw new DuplicateVehicleException(
                    "An active vehicle already uses number " + number);
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(number);
        vehicle.setCapacity(request.getCapacity());
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setActive(true);
        return VehicleMapper.toVehicleDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getAll() {
        return vehicleRepository.findAllByActiveTrueOrderByVehicleNumberAsc()
                .stream()
                .map(VehicleMapper::toVehicleDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDto get(Long vehicleId) {
        return VehicleMapper.toVehicleDto(requireVehicle(vehicleId));
    }

    @Override
    @Transactional
    public VehicleResponseDto update(Long vehicleId, VehicleRequestDto request) {
        Vehicle vehicle = requireVehicle(vehicleId);
        String number = normalizeNumber(request.getVehicleNumber());
        if (vehicleRepository
                .existsByVehicleNumberIgnoreCaseAndVehicleIdNot(
                        number, vehicleId)) {
            throw new DuplicateVehicleException(
                    "An active vehicle already uses number " + number);
        }
        vehicle.setVehicleNumber(number);
        vehicle.setCapacity(request.getCapacity());
        return VehicleMapper.toVehicleDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public VehicleResponseDto updateStatus(Long vehicleId, VehicleStatus status) {
        Vehicle vehicle = requireVehicle(vehicleId);
        if (status == VehicleStatus.MAINTENANCE
                && assignmentRepository.existsByVehicle_VehicleIdAndStatus(
                        vehicleId, AssignmentStatus.ACTIVE)) {
            throw new AssignmentConflictException(
                    "Complete or cancel active assignments before maintenance");
        }
        vehicle.setStatus(status);
        return VehicleMapper.toVehicleDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void delete(Long vehicleId) {
        Vehicle vehicle = requireVehicle(vehicleId);
        if (assignmentRepository.existsByVehicle_VehicleIdAndStatus(
                vehicleId, AssignmentStatus.ACTIVE)) {
            throw new AssignmentConflictException(
                    "Complete or cancel active assignments before deleting the vehicle");
        }
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }

    private Vehicle requireVehicle(Long vehicleId) {
        return vehicleRepository.findByVehicleIdAndActiveTrue(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Active vehicle not found with id: " + vehicleId));
    }

    private String normalizeNumber(String rawNumber) {
        return rawNumber.trim().toUpperCase();
    }
}
