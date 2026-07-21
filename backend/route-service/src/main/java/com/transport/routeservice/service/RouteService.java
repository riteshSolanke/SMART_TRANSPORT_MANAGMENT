package com.transport.routeservice.service;

import com.transport.routeservice.dto.request.RouteRequestDto;
import com.transport.routeservice.dto.response.FareResponseDto;
import com.transport.routeservice.dto.response.RouteResponseDto;
import com.transport.routeservice.dto.response.RouteSearchResponseDto;
import java.util.List;



public interface RouteService {

    RouteResponseDto createRoute(RouteRequestDto dto);

    RouteResponseDto getRouteById(Long routeId);

    List<RouteResponseDto> getAllRoutes();

    RouteResponseDto updateRoute(Long routeId, RouteRequestDto dto);

    void deleteRoute(Long routeId);

    List<RouteSearchResponseDto> searchRoutes(String from, String to);

    FareResponseDto getFare(Long routeId, Long sourceStopId, Long destinationStopId);
}