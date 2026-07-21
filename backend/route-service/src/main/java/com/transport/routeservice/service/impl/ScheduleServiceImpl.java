package com.transport.routeservice.service.impl;


import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.ScheduleRepository;

import com.transport.routeservice.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public ScheduleResponseDto addSchedule(Long routeId, ScheduleRequestDto dto) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));

        Schedule schedule = new Schedule();
        schedule.setRoute(route);
        schedule.setDepartureTime(dto.getDepartureTime());
        schedule.setArrivalTime(dto.getArrivalTime());
        schedule.setDaysOfWeek(dto.getDaysOfWeek());
        schedule.setActive(true);
        Schedule saved = scheduleRepository.save(schedule);
        return RouteMapper.toScheduleDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedulesByRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new RouteNotFoundException("Route not found with id: " + routeId);
        }
        return scheduleRepository.findByRoute_RouteIdAndActiveTrue(routeId).stream()
                .map(RouteMapper::toScheduleDto)
                .collect(Collectors.toList());
    }

}
