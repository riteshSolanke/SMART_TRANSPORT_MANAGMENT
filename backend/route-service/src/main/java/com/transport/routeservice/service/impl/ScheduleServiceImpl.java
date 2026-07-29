package com.transport.routeservice.service.impl;


import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.exception.RouteNotFoundException;
import com.transport.routeservice.exception.ScheduleNotFoundException;
import com.transport.routeservice.exception.DuplicateScheduleException;
import com.transport.routeservice.mapper.RouteMapper;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.ScheduleRepository;

import com.transport.routeservice.service.ScheduleService;
import com.transport.routeservice.util.ScheduleDays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public ScheduleResponseDto addSchedule(Long routeId,
                                           ScheduleRequestDto dto) {

        Route route = routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() ->
                        new RouteNotFoundException(
                                "Route not found with id: " + routeId));

        validateTimes(dto);
        String normalizedDays = ScheduleDays.normalize(dto.getDaysOfWeek());
        ensureNoDuplicate(routeId, null, dto, normalizedDays);

        Schedule schedule = new Schedule();

        schedule.setRoute(route);
        schedule.setDepartureTime(dto.getDepartureTime());
        schedule.setArrivalTime(dto.getArrivalTime());
        schedule.setDaysOfWeek(normalizedDays);
        schedule.setActive(true);

        Schedule saved = scheduleRepository.save(schedule);

        return RouteMapper.toScheduleDto(saved);
    }




    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getSchedulesByRoute(Long routeId) {

        requireActiveRoute(routeId);
        return scheduleRepository.findByRoute_RouteIdAndActiveTrue(routeId).stream()
                .sorted(Comparator.comparing(Schedule::getDepartureTime))
                .map(RouteMapper::toScheduleDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponseDto getSchedule(Long routeId, Long scheduleId) {
        requireActiveRoute(routeId);
        return RouteMapper.toScheduleDto(findActiveSchedule(routeId, scheduleId));
    }

    @Override
    @Transactional
    public ScheduleResponseDto updateSchedule(
            Long routeId, Long scheduleId, ScheduleRequestDto dto) {
        requireActiveRoute(routeId);
        Schedule schedule = findActiveSchedule(routeId, scheduleId);
        validateTimes(dto);
        String normalizedDays = ScheduleDays.normalize(dto.getDaysOfWeek());
        ensureNoDuplicate(routeId, scheduleId, dto, normalizedDays);

        schedule.setDepartureTime(dto.getDepartureTime());
        schedule.setArrivalTime(dto.getArrivalTime());
        schedule.setDaysOfWeek(normalizedDays);
        return RouteMapper.toScheduleDto(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public void deleteSchedule(Long routeId, Long scheduleId) {
        requireActiveRoute(routeId);
        Schedule schedule = findActiveSchedule(routeId, scheduleId);
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private Route requireActiveRoute(Long routeId) {
        return routeRepository.findByRouteIdAndActiveTrue(routeId)
                .orElseThrow(() -> new RouteNotFoundException(
                        "Route not found with id: " + routeId));
    }

    private Schedule findActiveSchedule(Long routeId, Long scheduleId) {
        return scheduleRepository
                .findByScheduleIdAndRoute_RouteIdAndActiveTrue(scheduleId, routeId)
                .orElseThrow(() -> new ScheduleNotFoundException(
                        "Active schedule not found with id " + scheduleId
                                + " for route " + routeId));
    }

    private void validateTimes(ScheduleRequestDto dto) {
        if (!dto.getArrivalTime().isAfter(dto.getDepartureTime())) {
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time");
        }
    }

    private void ensureNoDuplicate(
            Long routeId,
            Long ignoredScheduleId,
            ScheduleRequestDto dto,
            String normalizedDays) {
        boolean duplicate = scheduleRepository.findByRoute_RouteIdAndActiveTrue(routeId)
                .stream()
                .filter(schedule -> ignoredScheduleId == null
                        || !schedule.getScheduleId().equals(ignoredScheduleId))
                .anyMatch(schedule ->
                        schedule.getDepartureTime().equals(dto.getDepartureTime())
                                && ScheduleDays.overlaps(
                                schedule.getDaysOfWeek(), normalizedDays));
        if (duplicate) {
            throw new DuplicateScheduleException(
                    "An active schedule already exists for this departure time and days");
        }
    }
}
