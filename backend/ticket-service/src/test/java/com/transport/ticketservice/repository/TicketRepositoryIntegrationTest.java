package com.transport.ticketservice.repository;

import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
})
class TicketRepositoryIntegrationTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TicketRepository repository;

    @Test
    void countsOnlyCapacityHoldingPassengersForJourney() {
        LocalDate serviceDate = LocalDate.now().plusDays(1);
        persistTicket("PNR0000001", "booking-001", serviceDate,
                TicketStatus.BOOKED, 2);
        persistTicket("PNR0000002", "booking-002", serviceDate,
                TicketStatus.USED, 1);
        persistTicket("PNR0000003", "booking-003", serviceDate,
                TicketStatus.CANCELLED, 5);
        Ticket activeHold = persistTicket(
                "PNR0000004", "booking-004", serviceDate,
                TicketStatus.PENDING_PAYMENT, 3);
        activeHold.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(5));
        Ticket expiredHold = persistTicket(
                "PNR0000005", "booking-005", serviceDate,
                TicketStatus.PENDING_PAYMENT, 9);
        expiredHold.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        entityManager.flush();
        entityManager.clear();

        Long reserved = repository.countReservedPassengers(
                10L, 20L, serviceDate,
                EnumSet.of(TicketStatus.BOOKED, TicketStatus.USED),
                TicketStatus.PENDING_PAYMENT,
                LocalDateTime.now());

        assertThat(reserved).isEqualTo(6L);
        assertThat(repository.findByUserIdAndIdempotencyKey(
                42L, "booking-001")).isPresent();
    }

    private Ticket persistTicket(
            String pnr, String idempotencyKey, LocalDate serviceDate,
            TicketStatus status, int passengerCount) {
        Ticket ticket = Ticket.builder()
                .pnrNumber(pnr)
                .userId(42L)
                .routeId(10L)
                .scheduleId(20L)
                .sourceStopId(100L)
                .destinationStopId(200L)
                .serviceDate(serviceDate)
                .departureTime(LocalTime.NOON)
                .passengerCount(passengerCount)
                .unitFare(new BigDecimal("25.00"))
                .fareAmount(new BigDecimal("50.00"))
                .assignmentId(7L)
                .vehicleId(8L)
                .idempotencyKey(idempotencyKey)
                .requestHash("a".repeat(64))
                .status(status)
                .build();
        entityManager.persist(ticket);
        return ticket;
    }
}
