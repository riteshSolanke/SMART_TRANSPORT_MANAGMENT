package com.transport.routeservice.repository;


import com.transport.routeservice.entity.Route;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByRouteIdAndActiveTrue(Long routeId);

    List<Route> findByActiveTrue();

    Optional<Route> findByRouteIdAndActiveTrue(Long routeId);

    boolean existsByRouteNameIgnoreCase(String routeName);

    @Query("""
            SELECT DISTINCT r
            FROM Route r
            JOIN r.stops source
            JOIN r.stops destination
            WHERE r.active = true
              AND LOWER(TRIM(source.stopName)) = LOWER(:from)
              AND LOWER(TRIM(destination.stopName)) = LOWER(:to)
              AND source.sequenceOrder < destination.sequenceOrder
            ORDER BY r.routeName
            """)
    List<Route> findRoutesServingStopsInOrder(
            @Param("from") String from,
            @Param("to") String to);



}
