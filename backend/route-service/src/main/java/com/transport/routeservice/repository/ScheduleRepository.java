package com.transport.routeservice.repository;


import com.transport.routeservice.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRoute_RouteIdAndActiveTrue(Long routeId);
    List<Schedule> findByRoute_RouteId(Long routeId);

    Optional<Schedule> findByScheduleIdAndRoute_RouteIdAndActiveTrue(
            Long scheduleId, Long routeId);
}
