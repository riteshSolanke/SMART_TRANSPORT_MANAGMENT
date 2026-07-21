package com.transport.routeservice.controller;

import com.transport.routeservice.dto.request.RouteRequestDto;
import com.transport.routeservice.dto.response.ApiResponseDto;
import com.transport.routeservice.dto.response.FareResponseDto;
import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.RouteSearchResponseDto;
import com.transport.routeservice.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<RouteResponseDto> createRoute(@Valid @RequestBody RouteRequestDto dto) {
        return ApiResponseDto.success("Route created successfully", routeService.createRoute(dto));
    }

    @GetMapping
    public ApiResponseDto<List<RouteResponseDto>> getAllRoutes() {
        return ApiResponseDto.success(routeService.getAllRoutes());
    }

    @GetMapping("/{id}")
    public ApiResponseDto<RouteResponseDto> getRouteById(@PathVariable Long id) {
        return ApiResponseDto.success(routeService.getRouteById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
    public ApiResponseDto<RouteResponseDto> updateRoute(@PathVariable Long id,
                                                        @Valid @RequestBody RouteRequestDto dto) {
        return ApiResponseDto.success("Route updated successfully", routeService.updateRoute(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponseDto<String> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ApiResponseDto.success("Route deleted successfully", "OK");
    }

    @GetMapping("/search")
    public ApiResponseDto<List<RouteSearchResponseDto>> searchRoutes(@RequestParam String from,
                                                                     @RequestParam String to) {
        return ApiResponseDto.success(routeService.searchRoutes(from, to));
    }

    @GetMapping("/{id}/fare")
    public ApiResponseDto<FareResponseDto> getFare(@PathVariable Long id,
                                                   @RequestParam Long sourceStopId,
                                                   @RequestParam Long destinationStopId) {
        return ApiResponseDto.success(routeService.getFare(id, sourceStopId, destinationStopId));
    }
}
