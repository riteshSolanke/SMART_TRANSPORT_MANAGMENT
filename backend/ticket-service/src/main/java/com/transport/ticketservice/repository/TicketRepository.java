package com.transport.ticketservice.repository;

import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.repository.projection.RouteUsageAnalyticsProjection;
import com.transport.ticketservice.repository.projection.TicketStatusAnalyticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByPnrNumber(String pnrNumber);

    List<Ticket> findByUserIdOrderByBookedAtDesc(Long userId);

    List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);

    boolean existsByPnrNumber(String pnrNumber);

    Optional<Ticket> findByUserIdAndIdempotencyKey(
            Long userId, String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(t.passengerCount), 0)
            FROM Ticket t
            WHERE t.routeId = :routeId
              AND t.scheduleId = :scheduleId
              AND t.serviceDate = :serviceDate
              AND (
                    t.status IN :confirmedStatuses
                    OR (
                        t.status = :pendingStatus
                        AND t.paymentExpiresAt > :now
                    )
              )
            """)
    Long countReservedPassengers(
            @Param("routeId") Long routeId,
            @Param("scheduleId") Long scheduleId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("confirmedStatuses") Collection<TicketStatus> confirmedStatuses,
            @Param("pendingStatus") TicketStatus pendingStatus,
            @Param("now") java.time.LocalDateTime now);

    @Query("SELECT t FROM Ticket t WHERE t.routeId = :routeId AND t.status = :status")
    List<Ticket> findByRouteIdAndStatus(@Param("routeId") Long routeId,
                                        @Param("status") TicketStatus status);

    @Query("""
            SELECT t.status AS status,
                   COUNT(t) AS ticketCount,
                   COALESCE(SUM(t.passengerCount), 0) AS passengerCount,
                   COALESCE(SUM(t.fareAmount), 0) AS fareTotal
            FROM Ticket t
            WHERE t.serviceDate BETWEEN :from AND :to
            GROUP BY t.status
            """)
    List<TicketStatusAnalyticsProjection> summarizeByStatus(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT t.routeId AS routeId,
                   COUNT(t) AS ticketCount,
                   COALESCE(SUM(t.passengerCount), 0) AS passengerCount,
                   COALESCE(SUM(t.fareAmount), 0) AS fareTotal
            FROM Ticket t
            WHERE t.serviceDate BETWEEN :from AND :to
              AND t.status IN :statuses
            GROUP BY t.routeId
            ORDER BY SUM(t.passengerCount) DESC
            """)
    List<RouteUsageAnalyticsProjection> summarizeConfirmedRouteUsage(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") Collection<TicketStatus> statuses);
}
