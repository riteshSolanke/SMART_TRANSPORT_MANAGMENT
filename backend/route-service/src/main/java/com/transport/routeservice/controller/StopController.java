package com.transport.routeservice.controller;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.ApiResponseDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.service.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/routes/{routeId}/stops")
@RequiredArgsConstructor
public class StopController {
    private final StopService stopService;
    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<StopResponseDto> addStop(@PathVariable Long routeId,
                                                   @Valid @RequestBody StopRequestDto dto) {
        return ApiResponseDto.success("Stop added successfully", stopService.addStop(routeId, dto));
    }
    @GetMapping
    public ApiResponseDto<List<StopResponseDto>> getStopsByRoute(@PathVariable Long routeId) {
        return ApiResponseDto.success(stopService.getStopsByRoute(routeId));
    }
}