package com.transport.routeservice.controller;


import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ApiResponseDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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
        return ApiResponseDto.success("Schedule added successfully", scheduleService.addSchedule(routeId, dto));
    }

    @GetMapping
    public ApiResponseDto<List<ScheduleResponseDto>> getSchedulesByRoute(@PathVariable Long routeId) {
        return ApiResponseDto.success(scheduleService.getSchedulesByRoute(routeId));
    }
}
