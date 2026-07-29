package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.StopRequestDto;
import com.transport.routeservice.dto.response.StopResponseDto;

import java.util.List;

public interface StopService {


     StopResponseDto addStop(Long routeId, StopRequestDto dto);

     List<StopResponseDto> getStopsByRoute(Long routeId);

     StopResponseDto getStop(Long routeId, Long stopId);

     StopResponseDto updateStop(Long routeId, Long stopId, StopRequestDto dto);

     void deleteStop(Long routeId, Long stopId);
}
