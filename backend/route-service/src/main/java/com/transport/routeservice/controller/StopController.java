package com.transport.routeservice.controller;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.ApiResponseDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.service.StopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        return ApiResponseDto.success("Stop added successfully", stopService.addStop(routeId, dto), LocalDateTime.now());
    }
    @GetMapping
    public ApiResponseDto<List<StopResponseDto>> getStopsByRoute(@PathVariable Long routeId) {
        return ApiResponseDto.success(stopService.getStopsByRoute(routeId), LocalDateTime.now());
    }

    @GetMapping("/{stopId}")
    public ApiResponseDto<StopResponseDto> getStop(
            @PathVariable Long routeId,
            @PathVariable Long stopId) {
        return ApiResponseDto.success(
                stopService.getStop(routeId, stopId), LocalDateTime.now());
    }

    @RequestMapping(value = "/{stopId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<StopResponseDto> updateStop(
            @PathVariable Long routeId,
            @PathVariable Long stopId,
            @Valid @RequestBody StopRequestDto dto) {
        return ApiResponseDto.success(
                "Stop updated successfully",
                stopService.updateStop(routeId, stopId, dto),
                LocalDateTime.now());
    }

    @DeleteMapping("/{stopId}")
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<String> deleteStop(
            @PathVariable Long routeId,
            @PathVariable Long stopId) {
        stopService.deleteStop(routeId, stopId);
        return ApiResponseDto.success(
                "Stop deleted successfully", "OK", LocalDateTime.now());
    }
}
