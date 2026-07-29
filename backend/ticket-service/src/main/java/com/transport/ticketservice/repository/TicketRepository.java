package com.transport.ticketservice.repository;

import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByPnrNumber(String pnrNumber);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);

    boolean existsByPnrNumber(String pnrNumber);

    @Query("SELECT t FROM Ticket t WHERE t.routeId = :routeId AND t.status = :status")
    List<Ticket> findByRouteIdAndStatus(@Param("routeId") Long routeId,
                                        @Param("status") TicketStatus status);

}
