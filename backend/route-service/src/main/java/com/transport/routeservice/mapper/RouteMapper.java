package com.transport.routeservice.mapper;

import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.dto.response.StopResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.entity.Stop;
import java.util.List;
import java.util.stream.Collectors;



public class RouteMapper {

    public static StopResponseDto toStopDto(Stop stop) {
        return StopResponseDto.builder()
                .stopId(stop.getStopId())
                .stopName(stop.getStopName())
                .sequenceOrder(stop.getSequenceOrder())
                .distanceFromStart(stop.getDistanceFromStart())
                .build();
    }

    public static ScheduleResponseDto toScheduleDto(Schedule schedule) {
        return ScheduleResponseDto.builder()
                .scheduleId(schedule.getScheduleId())
                .departureTime(schedule.getDepartureTime())
                .arrivalTime(schedule.getArrivalTime())
                .daysOfWeek(schedule.getDaysOfWeek())
                .active(schedule.isActive())
                .build();

    }

    public static RouteResponseDto toRouteDto(Route route) {
        List<StopResponseDto> stopDtos = route.getStops() == null ? List.of() :
                route.getStops().stream().map(RouteMapper::toStopDto).collect(Collectors.toList());
        List<ScheduleResponseDto> scheduleDtos = route.getSchedules() == null ? List.of() :
                route.getSchedules().stream().map(RouteMapper::toScheduleDto).collect(Collectors.toList());

        return RouteResponseDto.builder()
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .startPoint(route.getStartPoint())
                .endPoint(route.getEndPoint())
                .active(route.isActive())
                .stops(stopDtos)
                .schedules(scheduleDtos)
                .build();
    }

    public static RouteResponseDto toRouteDtoWithoutRelations(Route route) {
        return RouteResponseDto.builder()
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .startPoint(route.getStartPoint())
                .endPoint(route.getEndPoint())
                .active(route.isActive())
                .stops(List.of())
                .schedules(List.of())
                .build();
    }
}
