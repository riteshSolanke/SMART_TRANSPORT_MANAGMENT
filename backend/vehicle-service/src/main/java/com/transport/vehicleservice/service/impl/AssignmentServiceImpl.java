package com.transport.vehicleservice.service.impl;

import com.transport.vehicleservice.dto.request.AssignmentRequestDto;
import com.transport.vehicleservice.dto.response.AssignmentResponseDto;
import com.transport.vehicleservice.entity.Vehicle;
import com.transport.vehicleservice.entity.VehicleAssignment;
import com.transport.vehicleservice.enums.AssignmentStatus;
import com.transport.vehicleservice.enums.VehicleStatus;
import com.transport.vehicleservice.exception.AssignmentConflictException;
import com.transport.vehicleservice.exception.AssignmentNotFoundException;
import com.transport.vehicleservice.exception.VehicleNotFoundException;
import com.transport.vehicleservice.mapper.VehicleMapper;
import com.transport.vehicleservice.repository.VehicleAssignmentRepository;
import com.transport.vehicleservice.repository.VehicleRepository;
import com.transport.vehicleservice.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {
    private final VehicleRepository vehicleRepository;
    private final VehicleAssignmentRepository assignmentRepository;
    private final RouteReferenceValidator routeReferenceValidator;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AssignmentResponseDto assign(
            Long vehicleId, AssignmentRequestDto request) {
        Vehicle vehicle = requireVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new AssignmentConflictException(
                    "A vehicle in maintenance cannot be assigned");
        }
        if (assignmentRepository.existsByVehicle_VehicleIdAndServiceDateAndStatus(
                vehicleId, request.getServiceDate(), AssignmentStatus.ACTIVE)) {
            throw new AssignmentConflictException(
                    "Vehicle already has an active assignment on this date");
        }
        if (assignmentRepository
                .existsByRouteIdAndScheduleIdAndServiceDateAndStatus(
                        request.getRouteId(), request.getScheduleId(),
                        request.getServiceDate(), AssignmentStatus.ACTIVE)) {
            throw new AssignmentConflictException(
                    "Schedule already has an active vehicle on this date");
        }

        routeReferenceValidator.validate(
                request.getRouteId(), request.getScheduleId(),
                request.getServiceDate());

        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setVehicle(vehicle);
        assignment.setRouteId(request.getRouteId());
        assignment.setScheduleId(request.getScheduleId());
        assignment.setServiceDate(request.getServiceDate());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        return VehicleMapper.toAssignmentDto(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getHistory(Long vehicleId) {
        requireVehicle(vehicleId);
        return assignmentRepository
                .findByVehicle_VehicleIdOrderByServiceDateDescAssignedAtDesc(vehicleId)
                .stream()
                .map(VehicleMapper::toAssignmentDto)
                .toList();
    }

    @Override
    @Transactional
    public AssignmentResponseDto complete(Long vehicleId, Long assignmentId) {
        return finish(vehicleId, assignmentId, AssignmentStatus.COMPLETED);
    }

    @Override
    @Transactional
    public AssignmentResponseDto cancel(Long vehicleId, Long assignmentId) {
        return finish(vehicleId, assignmentId, AssignmentStatus.CANCELLED);
    }

    private AssignmentResponseDto finish(
            Long vehicleId, Long assignmentId, AssignmentStatus newStatus) {
        requireVehicle(vehicleId);
        VehicleAssignment assignment = assignmentRepository
                .findByAssignmentIdAndVehicle_VehicleIdAndStatus(
                        assignmentId, vehicleId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new AssignmentNotFoundException(
                        "Active assignment not found for this vehicle"));
        assignment.setStatus(newStatus);
        assignment.setCompletedAt(LocalDateTime.now());
        return VehicleMapper.toAssignmentDto(assignmentRepository.save(assignment));
    }

    private Vehicle requireVehicle(Long vehicleId) {
        return vehicleRepository.findByVehicleIdAndActiveTrue(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(
                        "Active vehicle not found with id: " + vehicleId));
    }
}
