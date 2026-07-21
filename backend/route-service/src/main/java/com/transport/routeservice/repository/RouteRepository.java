package com.transport.routeservice.repository;


import com.transport.routeservice.entity.Route;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByActiveTrue();

    Optional<Route> findByRouteIdAndActiveTrue(Long routeId);

    boolean existsByRouteNameIgnoreCase(String routeName);

    @Query("SELECT r FROM Route r WHERE r.active = true " +

            "AND LOWER(r.startPoint) = LOWER(:from) AND LOWER(r.endPoint) = LOWER(:to)")

    List<Route> findDirectRoutes(@Param("from") String from, @Param("to") String to);

}
