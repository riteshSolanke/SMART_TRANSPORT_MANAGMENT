package com.transport.vehicleservice.service;

import com.transport.vehicleservice.dto.request.AssignmentRequestDto;
import com.transport.vehicleservice.dto.response.AssignmentResponseDto;
import com.transport.vehicleservice.dto.response.VehicleAvailabilityResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentService {
    AssignmentResponseDto assign(Long vehicleId, AssignmentRequestDto request);
    List<AssignmentResponseDto> getHistory(Long vehicleId);
    AssignmentResponseDto complete(Long vehicleId, Long assignmentId);
    AssignmentResponseDto cancel(Long vehicleId, Long assignmentId);
    VehicleAvailabilityResponseDto getAvailability(
            Long routeId, Long scheduleId, LocalDate serviceDate);
}
