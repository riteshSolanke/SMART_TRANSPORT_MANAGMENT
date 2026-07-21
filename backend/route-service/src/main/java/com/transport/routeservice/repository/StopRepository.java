package com.transport.routeservice.repository;


import com.transport.routeservice.entity.Stop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findByRoute_RouteIdOrderBySequenceOrderAsc(Long routeId);

    boolean existsByRoute_RouteIdAndSequenceOrder(Long routeId, Integer sequenceOrder);

    Optional<Stop> findByStopIdAndRoute_RouteId(Long stopId, Long routeId);

}
