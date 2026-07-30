package com.transport.vehicleservice.controller;

import com.transport.vehicleservice.dto.request.*;
import com.transport.vehicleservice.dto.response.*;
import com.transport.vehicleservice.service.AssignmentService;
import com.transport.vehicleservice.service.TrackingService;
import com.transport.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService vehicleService;
    private final AssignmentService assignmentService;
    private final TrackingService trackingService;

    @GetMapping("/assignments/availability")
    public ApiResponseDto<VehicleAvailabilityResponseDto> availability(
            @RequestParam Long routeId,
            @RequestParam Long scheduleId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate serviceDate) {
        return ApiResponseDto.success(
                assignmentService.getAvailability(routeId, scheduleId, serviceDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<VehicleResponseDto> create(
            @Valid @RequestBody VehicleRequestDto request) {
        return ApiResponseDto.success(
                "Vehicle created successfully", vehicleService.create(request));
    }

    @GetMapping
    public ApiResponseDto<List<VehicleResponseDto>> getAll() {
        return ApiResponseDto.success(vehicleService.getAll());
    }

    @GetMapping("/{vehicleId}")
    public ApiResponseDto<VehicleResponseDto> get(@PathVariable Long vehicleId) {
        return ApiResponseDto.success(vehicleService.get(vehicleId));
    }

    @RequestMapping(value = "/{vehicleId}",
            method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<VehicleResponseDto> update(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleRequestDto request) {
        return ApiResponseDto.success(
                "Vehicle updated successfully",
                vehicleService.update(vehicleId, request));
    }

    @PatchMapping("/{vehicleId}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<VehicleResponseDto> updateStatus(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleStatusRequestDto request) {
        return ApiResponseDto.success(
                "Vehicle status updated successfully",
                vehicleService.updateStatus(vehicleId, request.getStatus()));
    }

    @DeleteMapping("/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDto<String> delete(@PathVariable Long vehicleId) {
        vehicleService.delete(vehicleId);
        return ApiResponseDto.success("Vehicle deleted successfully", "OK");
    }

    @PostMapping("/{vehicleId}/assignments")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<AssignmentResponseDto> assign(
            @PathVariable Long vehicleId,
            @Valid @RequestBody AssignmentRequestDto request) {
        return ApiResponseDto.success(
                "Vehicle assigned successfully",
                assignmentService.assign(vehicleId, request));
    }

    @GetMapping("/{vehicleId}/assignments")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<List<AssignmentResponseDto>> assignmentHistory(
            @PathVariable Long vehicleId) {
        return ApiResponseDto.success(assignmentService.getHistory(vehicleId));
    }

    @PatchMapping("/{vehicleId}/assignments/{assignmentId}/complete")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<AssignmentResponseDto> complete(
            @PathVariable Long vehicleId, @PathVariable Long assignmentId) {
        return ApiResponseDto.success(
                "Assignment completed successfully",
                assignmentService.complete(vehicleId, assignmentId));
    }

    @PatchMapping("/{vehicleId}/assignments/{assignmentId}/cancel")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<AssignmentResponseDto> cancel(
            @PathVariable Long vehicleId, @PathVariable Long assignmentId) {
        return ApiResponseDto.success(
                "Assignment cancelled successfully",
                assignmentService.cancel(vehicleId, assignmentId));
    }

    @PostMapping("/{vehicleId}/locations")
    @PreAuthorize("hasAnyRole('CONDUCTOR','DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<LocationResponseDto> recordLocation(
            @PathVariable Long vehicleId,
            @Valid @RequestBody LocationRequestDto request) {
        return ApiResponseDto.success(
                "Location recorded successfully",
                trackingService.record(vehicleId, request));
    }

    @GetMapping("/{vehicleId}/locations/latest")
    public ApiResponseDto<LocationResponseDto> latestLocation(
            @PathVariable Long vehicleId) {
        return ApiResponseDto.success(trackingService.latest(vehicleId));
    }

    @GetMapping("/{vehicleId}/locations")
    @PreAuthorize("hasAnyRole('DISPATCHER','TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<List<LocationResponseDto>> locationHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponseDto.success(trackingService.history(vehicleId, limit));
    }
}
