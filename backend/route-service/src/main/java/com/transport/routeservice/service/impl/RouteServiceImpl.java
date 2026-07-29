package com.transport.routeservice.service.impl;


import com.transport.routeservice.dto.request.RouteRequestDto;
import com.transport.routeservice.dto.response.FareResponseDto;
import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.RouteSearchResponseDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.entity.Stop;
import com.transport.routeservice.exception.DuplicateRouteException;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.exception.ScheduleNotFoundException;
import com.transport.routeservice.exception.StopNotFoundException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.ScheduleRepository;
import com.transport.routeservice.repository.StopRepository;
import com.transport.routeservice.service.FareCalculationService;
import com.transport.routeservice.service.RouteService;
import com.transport.routeservice.util.ScheduleDays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor

public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final FareCalculationService fareCalculationService;
    private final StopRepository stopRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public RouteResponseDto createRoute(RouteRequestDto dto) {
        String routeName = dto.getRouteName().trim();
        String startPoint = dto.getStartPoint().trim();
        String endPoint = dto.getEndPoint().trim();
        validateEndpoints(startPoint, endPoint);

        if (routeRepository.existsByRouteNameIgnoreCase(routeName)) {
            throw new DuplicateRouteException(
                    "A route with name '" + routeName + "' already exists");
        }
        Route route = new Route();
        route.setRouteName(routeName);
        route.setStartPoint(startPoint);
        route.setEndPoint(endPoint);
        route.setActive(true);
        Route saved = routeRepository.save(route);
        return RouteMapper.toRouteDtoWithoutRelations(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponseDto getRouteById(Long routeId) {
        Route route = routeRepository.findByRouteIdAndActiveTrue(routeId)
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

        Route route = routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));

        String routeName = dto.getRouteName().trim();
        String startPoint = dto.getStartPoint().trim();
        String endPoint = dto.getEndPoint().trim();
        validateEndpoints(startPoint, endPoint);

        if (!route.getRouteName().equalsIgnoreCase(routeName)
                && routeRepository.existsByRouteNameIgnoreCase(routeName)) {

            throw new DuplicateRouteException(
                    "A route with name '" + routeName + "' already exists");
        }

        route.setRouteName(routeName);
        route.setStartPoint(startPoint);
        route.setEndPoint(endPoint);

        Route updated = routeRepository.save(route);

        return RouteMapper.toRouteDtoWithoutRelations(updated);
    }


    @Override
    @Transactional
    public void deleteRoute(Long routeId) {
        Route route = routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
        route.setActive(false);
        route.getSchedules().forEach(schedule -> schedule.setActive(false));
        routeRepository.save(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteSearchResponseDto> searchRoutes(
            String from, String to, LocalDate travelDate) {
        String normalizedFrom = normalizeSearchTerm(from, "Source stop");
        String normalizedTo = normalizeSearchTerm(to, "Destination stop");
        if (normalizedFrom.equalsIgnoreCase(normalizedTo)) {
            throw new IllegalArgumentException(
                    "Source and destination stops must be different");
        }

        return routeRepository
                .findRoutesServingStopsInOrder(normalizedFrom, normalizedTo)
                .stream()
                .map(route -> toSearchResult(
                        route, normalizedFrom, normalizedTo, travelDate))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .sorted(Comparator
                        .comparing(RouteSearchResponseDto::getTotalDistanceKm)
                        .thenComparing(RouteSearchResponseDto::getRouteName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FareResponseDto getFare(
            Long routeId,
            Long sourceStopId,
            Long destinationStopId,
            Long scheduleId,
            LocalTime departureTime) {
        routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with id: " + routeId));
        Stop sourceStop = stopRepository
                .findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(sourceStopId, routeId)
                .orElseThrow(() -> new StopNotFoundException(
                        "Source stop not found for route id: " + routeId));
        Stop destinationStop = stopRepository
                .findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(destinationStopId, routeId)
                .orElseThrow(() -> new StopNotFoundException(
                        "Destination stop not found for route id: " + routeId));
        if (destinationStop.getSequenceOrder() <= sourceStop.getSequenceOrder()) {
            throw new IllegalArgumentException("Destination stop must come after source stop");
        }

        BigDecimal distance = destinationStop.getDistanceFromStart()
                .subtract(sourceStop.getDistanceFromStart());
        if (distance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Destination distance must be greater than source distance");
        }
        if (scheduleId != null && departureTime != null) {
            throw new IllegalArgumentException(
                    "Provide either scheduleId or departureTime, not both");
        }

        LocalTime effectiveDepartureTime = departureTime;
        if (scheduleId != null) {
            Schedule schedule = scheduleRepository
                    .findByScheduleIdAndRoute_RouteIdAndActiveTrue(scheduleId, routeId)
                    .orElseThrow(() -> new ScheduleNotFoundException(
                            "Active schedule not found with id " + scheduleId
                                    + " for route " + routeId));
            effectiveDepartureTime = schedule.getDepartureTime();
        }

        boolean peak = fareCalculationService.isPeakHour(effectiveDepartureTime);
        BigDecimal fare = fareCalculationService.calculateFare(distance, peak);
        return FareResponseDto.builder()
                .routeId(routeId)
                .sourceStopId(sourceStopId)
                .destinationStopId(destinationStopId)
                .scheduleId(scheduleId)
                .departureTime(effectiveDepartureTime)
                .distanceKm(distance)
                .fare(fare)
                .peakHourApplied(peak)
                .build();
    }

    private java.util.Optional<RouteSearchResponseDto> toSearchResult(
            Route route, String from, String to, LocalDate travelDate) {
        RouteSegment segment = findShortestMatchingSegment(route, from, to);
        if (segment == null) {
            return java.util.Optional.empty();
        }

        List<ScheduleResponseDto> schedules = route.getSchedules().stream()
                .filter(Schedule::isActive)
                .filter(schedule -> travelDate == null
                        || ScheduleDays.runsOn(
                        schedule.getDaysOfWeek(), travelDate.getDayOfWeek()))
                .sorted(Comparator.comparing(Schedule::getDepartureTime))
                .map(RouteMapper::toScheduleDto)
                .toList();
        if (travelDate != null && schedules.isEmpty()) {
            return java.util.Optional.empty();
        }

        BigDecimal distance = segment.destination().getDistanceFromStart()
                .subtract(segment.source().getDistanceFromStart());
        return java.util.Optional.of(RouteSearchResponseDto.builder()
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .startPoint(route.getStartPoint())
                .endPoint(route.getEndPoint())
                .sourceStopId(segment.source().getStopId())
                .sourceStopName(segment.source().getStopName())
                .destinationStopId(segment.destination().getStopId())
                .destinationStopName(segment.destination().getStopName())
                .totalDistanceKm(distance)
                .estimatedFare(fareCalculationService.calculateFare(distance, false))
                .availableSchedules(schedules)
                .build());
    }

    private RouteSegment findShortestMatchingSegment(Route route, String from, String to) {
        List<Stop> sourceStops = route.getStops().stream()
                .filter(stop -> stop.getStopName().trim().equalsIgnoreCase(from))
                .toList();
        return sourceStops.stream()
                .flatMap(source -> route.getStops().stream()
                        .filter(destination ->
                                destination.getStopName().trim().equalsIgnoreCase(to))
                        .filter(destination ->
                                destination.getSequenceOrder() > source.getSequenceOrder())
                        .map(destination -> new RouteSegment(source, destination)))
                .min(Comparator.comparing(segment ->
                        segment.destination().getDistanceFromStart()
                                .subtract(segment.source().getDistanceFromStart())))
                .orElse(null);
    }

    private String normalizeSearchTerm(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private void validateEndpoints(String startPoint, String endPoint) {
        if (startPoint.equalsIgnoreCase(endPoint)) {
            throw new IllegalArgumentException(
                    "Route start point and end point must be different");
        }
    }

    private record RouteSegment(Stop source, Stop destination) {
    }
}
