package com.transport.routeservice.controller;


import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ApiResponseDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/routes/{routeId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<ScheduleResponseDto> addSchedule(@PathVariable Long routeId,
                                                           @Valid @RequestBody ScheduleRequestDto dto) {
        return ApiResponseDto.success("Schedule added successfully", scheduleService.addSchedule(routeId, dto), LocalDateTime.now());
    }

    @GetMapping
    public ApiResponseDto<List<ScheduleResponseDto>> getSchedulesByRoute(@PathVariable Long routeId) {
        return ApiResponseDto.success(scheduleService.getSchedulesByRoute(routeId), LocalDateTime.now());
    }

    @GetMapping("/{scheduleId}")
    public ApiResponseDto<ScheduleResponseDto> getSchedule(
            @PathVariable Long routeId,
            @PathVariable Long scheduleId) {
        return ApiResponseDto.success(
                scheduleService.getSchedule(routeId, scheduleId), LocalDateTime.now());
    }

    @RequestMapping(value = "/{scheduleId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<ScheduleResponseDto> updateSchedule(
            @PathVariable Long routeId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequestDto dto) {
        return ApiResponseDto.success(
                "Schedule updated successfully",
                scheduleService.updateSchedule(routeId, scheduleId, dto),
                LocalDateTime.now());
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<String> deleteSchedule(
            @PathVariable Long routeId,
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(routeId, scheduleId);
        return ApiResponseDto.success(
                "Schedule deactivated successfully", "OK", LocalDateTime.now());
    }
}

