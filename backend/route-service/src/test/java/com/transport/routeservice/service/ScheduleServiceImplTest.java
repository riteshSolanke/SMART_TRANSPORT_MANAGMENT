package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;
import com.transport.routeservice.entity.Route;
import com.transport.routeservice.entity.Schedule;
import com.transport.routeservice.exception.DuplicateScheduleException;
import com.transport.routeservice.repository.RouteRepository;
import com.transport.routeservice.repository.ScheduleRepository;
import com.transport.routeservice.service.impl.ScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private RouteRepository routeRepository;

    private ScheduleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScheduleServiceImpl(scheduleRepository, routeRepository);
    }

    @Test
    void addScheduleNormalizesOperatingDays() {
        Route route = activeRoute();
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(route));
        when(scheduleRepository.findByRoute_RouteIdAndActiveTrue(10L))
                .thenReturn(List.of());
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponseDto response =
                service.addSchedule(10L, request("fri, mon"));

        assertThat(response.getDaysOfWeek()).isEqualTo("MON,FRI");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void rejectsUnknownOperatingDay() {
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(activeRoute()));

        assertThatThrownBy(() ->
                service.addSchedule(10L, request("MON,FUNDAY")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MON,TUE");

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateActiveSchedule() {
        Schedule existing = new Schedule();
        existing.setScheduleId(1L);
        existing.setDepartureTime(LocalTime.of(9, 0));
        existing.setDaysOfWeek("MON,FRI");
        existing.setActive(true);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(activeRoute()));
        when(scheduleRepository.findByRoute_RouteIdAndActiveTrue(10L))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() ->
                service.addSchedule(10L, request("MON,FRI")))
                .isInstanceOf(DuplicateScheduleException.class);
    }

    @Test
    void rejectsSameDepartureWhenOperatingDaysOverlap() {
        Schedule existing = new Schedule();
        existing.setScheduleId(1L);
        existing.setDepartureTime(LocalTime.of(9, 0));
        existing.setDaysOfWeek("MON,WED,FRI");
        existing.setActive(true);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(activeRoute()));
        when(scheduleRepository.findByRoute_RouteIdAndActiveTrue(10L))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() ->
                service.addSchedule(10L, request("FRI,SUN")))
                .isInstanceOf(DuplicateScheduleException.class);
    }

    @Test
    void deleteScheduleUsesSoftDeactivation() {
        Schedule schedule = new Schedule();
        schedule.setScheduleId(5L);
        schedule.setActive(true);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(activeRoute()));
        when(scheduleRepository
                .findByScheduleIdAndRoute_RouteIdAndActiveTrue(5L, 10L))
                .thenReturn(Optional.of(schedule));

        service.deleteSchedule(10L, 5L);

        assertThat(schedule.isActive()).isFalse();
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void updateScheduleExcludesItselfFromDuplicateCheck() {
        Schedule schedule = new Schedule();
        schedule.setScheduleId(5L);
        schedule.setDepartureTime(LocalTime.of(9, 0));
        schedule.setArrivalTime(LocalTime.of(10, 0));
        schedule.setDaysOfWeek("MON");
        schedule.setActive(true);
        when(routeRepository.findByRouteIdAndActiveTrue(10L))
                .thenReturn(Optional.of(activeRoute()));
        when(scheduleRepository
                .findByScheduleIdAndRoute_RouteIdAndActiveTrue(5L, 10L))
                .thenReturn(Optional.of(schedule));
        when(scheduleRepository.findByRoute_RouteIdAndActiveTrue(10L))
                .thenReturn(List.of(schedule));
        when(scheduleRepository.save(schedule)).thenReturn(schedule);
        ScheduleRequestDto update = request("wed, mon");
        update.setDepartureTime(LocalTime.of(10, 30));
        update.setArrivalTime(LocalTime.of(11, 30));

        ScheduleResponseDto response =
                service.updateSchedule(10L, 5L, update);

        assertThat(response.getDepartureTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(response.getDaysOfWeek()).isEqualTo("MON,WED");
    }

    private Route activeRoute() {
        Route route = new Route();
        route.setRouteId(10L);
        route.setActive(true);
        return route;
    }

    private ScheduleRequestDto request(String days) {
        ScheduleRequestDto dto = new ScheduleRequestDto();
        dto.setDepartureTime(LocalTime.of(9, 0));
        dto.setArrivalTime(LocalTime.of(10, 0));
        dto.setDaysOfWeek(days);
        return dto;
    }
}
