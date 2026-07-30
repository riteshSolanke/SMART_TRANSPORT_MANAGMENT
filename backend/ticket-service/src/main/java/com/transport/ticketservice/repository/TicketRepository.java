package com.transport.ticketservice.repository;

import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
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

}
