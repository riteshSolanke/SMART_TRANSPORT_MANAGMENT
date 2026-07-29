package com.transport.routeservice.service.impl;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Stop;
import com.transport.routeservice.exception.DuplicateStopSequenceException;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.exception.StopNotFoundException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.StopRepository;

import com.transport.routeservice.service.StopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StopServiceImpl implements StopService {
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public StopResponseDto addStop(Long routeId,
                                   StopRequestDto dto) {

        Route route = routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));

        List<Stop> existingStops =
                stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(routeId);
        validatePosition(existingStops, dto);

        Stop stop = new Stop();

        stop.setRoute(route);
        stop.setStopName(dto.getStopName().trim());
        stop.setSequenceOrder(dto.getSequenceOrder());
        stop.setDistanceFromStart(dto.getDistanceFromStart());

        Stop saved = stopRepository.save(stop);

        return RouteMapper.toStopDto(saved);
    }


    @Transactional(readOnly = true)
    public List<StopResponseDto> getStopsByRoute(Long routeId) {
        requireActiveRoute(routeId);
        return stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(routeId).stream()
                .map(RouteMapper::toStopDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StopResponseDto getStop(Long routeId, Long stopId) {
        requireActiveRoute(routeId);
        return RouteMapper.toStopDto(findStop(routeId, stopId));
    }

    @Override
    @Transactional
    public StopResponseDto updateStop(
            Long routeId, Long stopId, StopRequestDto dto) {
        requireActiveRoute(routeId);
        Stop stop = findStop(routeId, stopId);
        List<Stop> otherStops =
                stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(routeId).stream()
                        .filter(existing -> !Objects.equals(existing.getStopId(), stopId))
                        .toList();
        validatePosition(otherStops, dto);

        stop.setStopName(dto.getStopName().trim());
        stop.setSequenceOrder(dto.getSequenceOrder());
        stop.setDistanceFromStart(dto.getDistanceFromStart());
        return RouteMapper.toStopDto(stopRepository.save(stop));
    }

    @Override
    @Transactional
    public void deleteStop(Long routeId, Long stopId) {
        requireActiveRoute(routeId);
        stopRepository.delete(findStop(routeId, stopId));
    }

    private Route requireActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
    }

    private Stop findStop(Long routeId, Long stopId) {
        return stopRepository
                .findByStopIdAndRoute_RouteIdAndRoute_ActiveTrue(stopId, routeId)
                .orElseThrow(() -> new StopNotFoundException(
                        "Stop not found with id " + stopId + " for route " + routeId));
    }

    private void validatePosition(List<Stop> existingStops, StopRequestDto dto) {
        Stop sameSequence = existingStops.stream()
                .filter(stop -> stop.getSequenceOrder().equals(dto.getSequenceOrder()))
                .findFirst()
                .orElse(null);
        if (sameSequence != null) {
            throw new DuplicateStopSequenceException(
                    "A stop already exists at sequence order " + dto.getSequenceOrder());
        }

        Stop previous = existingStops.stream()
                .filter(stop -> stop.getSequenceOrder() < dto.getSequenceOrder())
                .max(java.util.Comparator.comparing(Stop::getSequenceOrder))
                .orElse(null);
        Stop next = existingStops.stream()
                .filter(stop -> stop.getSequenceOrder() > dto.getSequenceOrder())
                .min(java.util.Comparator.comparing(Stop::getSequenceOrder))
                .orElse(null);

        if (previous != null && dto.getDistanceFromStart()
                .compareTo(previous.getDistanceFromStart()) <= 0) {
            throw new IllegalArgumentException(
                    "Distance from start must be greater than the previous stop distance");
        }
        if (next != null && dto.getDistanceFromStart()
                .compareTo(next.getDistanceFromStart()) >= 0) {
            throw new IllegalArgumentException(
                    "Distance from start must be less than the next stop distance");
        }
    }
}
