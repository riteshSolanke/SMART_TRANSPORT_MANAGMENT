package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.ScheduleRequestDto;
import com.transport.routeservice.dto.response.ScheduleResponseDto;

import java.util.List;

public interface ScheduleService {

    ScheduleResponseDto addSchedule(Long routeId, ScheduleRequestDto dto);

    List<ScheduleResponseDto> getSchedulesByRoute(Long routeId);

}
