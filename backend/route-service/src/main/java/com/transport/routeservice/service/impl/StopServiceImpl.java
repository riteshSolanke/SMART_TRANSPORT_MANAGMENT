package com.transport.routeservice.service.impl;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Stop;
import com.transport.routeservice.exception.DuplicateStopSequenceException;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.StopRepository;

import com.transport.routeservice.service.StopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StopServiceImpl implements StopService {
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public StopResponseDto addStop(Long routeId, StopRequestDto dto) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
        if (stopRepository.existsByRoute_RouteIdAndSequenceOrder(routeId, dto.getSequenceOrder())) {
            throw new DuplicateStopSequenceException(
                    "A stop already exists at sequence order " + dto.getSequenceOrder()
                            + " for this route");
        }
        Stop stop = new Stop();
        stop.setRoute(route);
        stop.setStopName(dto.getStopName());
        stop.setSequenceOrder(dto.getSequenceOrder());
        stop.setDistanceFromStart(dto.getDistanceFromStart());
        Stop saved = stopRepository.save(stop);
        return RouteMapper.toStopDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StopResponseDto> getStopsByRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new RouteNotFoundException("Route not found with id: " + routeId);
        }
        return stopRepository.findByRoute_RouteIdOrderBySequenceOrderAsc(routeId).stream()
                .map(RouteMapper::toStopDto)
                .collect(Collectors.toList());
    }

}
