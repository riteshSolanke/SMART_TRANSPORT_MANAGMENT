package com.transport.routeservice.service.impl;


import com.transport.routeservice.dto.request.RouteRequestDto;
import com.transport.routeservice.dto.response.FareResponseDto;
import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.RouteSearchResponseDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.exception.DuplicateRouteException;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.StopRepository;
import com.transport.routeservice.service.FareCalculationService;
import com.transport.routeservice.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor

public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final FareCalculationService fareCalculationService;
    private final StopRepository stopRepository;

    @Override
    @Transactional
    public RouteResponseDto createRoute(RouteRequestDto dto) {
        if (routeRepository.existsByRouteNameIgnoreCase(dto.getRouteName())) {
            throw new DuplicateRouteException(
                    "A route with name '" + dto.getRouteName() + "' already exists");
        }
        Route route = new Route();
        route.setRouteName(dto.getRouteName());
        route.setStartPoint(dto.getStartPoint());
        route.setEndPoint(dto.getEndPoint());
        route.setActive(true);
        Route saved = routeRepository.save(route);
        return RouteMapper.toRouteDtoWithoutRelations(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponseDto getRouteById(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
        return RouteMapper.toRouteDto(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponseDto> getAllRoutes() {
        return routeRepository.findByActiveTrue().stream()
                .map(RouteMapper::toRouteDtoWithoutRelations)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RouteResponseDto updateRoute(Long routeId, RouteRequestDto dto) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
        route.setRouteName(dto.getRouteName());
        route.setStartPoint(dto.getStartPoint());
        route.setEndPoint(dto.getEndPoint());
        Route updated = routeRepository.save(route);
        return RouteMapper.toRouteDtoWithoutRelations(updated);
    }

    @Override
    @Transactional
    public void deleteRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
        route.setActive(false);
        routeRepository.save(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteSearchResponseDto> searchRoutes(String from, String to) {
        List<Route> directRoutes = routeRepository.findDirectRoutes(from, to);
        return directRoutes.stream().map(route -> {
            BigDecimal totalDistance = route.getStops().isEmpty()
                    ? BigDecimal.ZERO
                    : route.getStops().get(route.getStops().size() - 1).getDistanceFromStart();
            BigDecimal fare = fareCalculationService.calculateFare(totalDistance, false);
            List<ScheduleResponseDto> schedules = route.getSchedules().stream()
                    .filter(s -> s.isActive())
                    .map(RouteMapper::toScheduleDto)
                    .collect(Collectors.toList());
            return RouteSearchResponseDto.builder()
                    .routeId(route.getRouteId())
                    .routeName(route.getRouteName())
                    .startPoint(route.getStartPoint())
                    .endPoint(route.getEndPoint())
                    .totalDistanceKm(totalDistance)
                    .estimatedFare(fare)
                    .availableSchedules(schedules)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FareResponseDto getFare(Long routeId, Long sourceStopId, Long destinationStopId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with id: " + routeId));
        var sourceStop = stopRepository.findByStopIdAndRoute_RouteId(sourceStopId, routeId)
                .orElseThrow(() -> new RouteNotFoundException("Source stop not found for route id: " + routeId));
        var destinationStop = stopRepository.findByStopIdAndRoute_RouteId(destinationStopId, routeId)
                .orElseThrow(() -> new RouteNotFoundException("Destination stop not found for route id: " + routeId));
        if (destinationStop.getDistanceFromStart().compareTo(sourceStop.getDistanceFromStart()) < 0) {
            throw new IllegalArgumentException("Destination stop must come after source stop");
        }

        BigDecimal distance = destinationStop.getDistanceFromStart()
                .subtract(sourceStop.getDistanceFromStart());
        boolean peak = route.getSchedules().stream()
                .anyMatch(schedule -> fareCalculationService.isPeakHour(schedule.getDepartureTime()));
        BigDecimal fare = fareCalculationService.calculateFare(distance, peak);
        return FareResponseDto.builder()
                .routeId(routeId)
                .sourceStopId(sourceStopId)
                .destinationStopId(destinationStopId)
                .distanceKm(distance)
                .fare(fare)
                .peakHourApplied(peak)
                .build();
    }
}
