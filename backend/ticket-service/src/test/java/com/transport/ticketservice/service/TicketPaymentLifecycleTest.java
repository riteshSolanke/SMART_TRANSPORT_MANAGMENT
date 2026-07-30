package com.transport.ticketservice.service;

import com.transport.ticketservice.client.RouteServiceClient;
import com.transport.ticketservice.client.VehicleServiceClient;
import com.transport.ticketservice.dto.request.PaymentUpdateRequestDto;
import com.transport.ticketservice.entity.Ticket;
import com.transport.ticketservice.enums.TicketStatus;
import com.transport.ticketservice.exception.InvalidTicketStateException;
import com.transport.ticketservice.mapper.TicketMapper;
import com.transport.ticketservice.repository.TicketRepository;
import com.transport.ticketservice.service.impl.TicketServiceImpl;
import com.transport.ticketservice.util.BookingRequestHasher;
import com.transport.ticketservice.util.TicketNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketPaymentLifecycleTest {
    @Mock
    private TicketRepository repository;
    @Mock
    private RouteServiceClient routeClient;
    @Mock
    private VehicleServiceClient vehicleClient;
    @Mock
    private TicketNumberGenerator numberGenerator;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                repository,
                new TicketMapper(),
                routeClient,
                vehicleClient,
                numberGenerator,
                new BookingRequestHasher());
        lenient().when(repository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void activeHoldIsPayable() {
        Ticket ticket = pendingTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        var context =
                service.getPaymentContext(1L, 42L, false);

        assertThat(context.isPayable()).isTrue();
        assertThat(context.isRefundable()).isFalse();
        assertThat(context.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void expiredHoldIsReleasedWhenPaymentContextIsRead() {
        Ticket ticket = pendingTicket();
        ticket.setPaymentExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        var context =
                service.getPaymentContext(1L, 42L, false);

        assertThat(context.getStatus()).isEqualTo(TicketStatus.EXPIRED);
        assertThat(context.isPayable()).isFalse();
        verify(repository).save(ticket);
    }

    @Test
    void successfulPaymentConfirmsTicket() {
        Ticket ticket = pendingTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        var response = service.confirmPayment(
                1L, paymentUpdate(), 42L, false);

        assertThat(response.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(response.getPaymentId()).isEqualTo(10L);
        assertThat(response.getPaidAt()).isNotNull();
        assertThat(response.getPaymentExpiresAt()).isNull();
    }

    @Test
    void mismatchedPaymentAmountIsRejected() {
        Ticket ticket = pendingTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        PaymentUpdateRequestDto update = paymentUpdate();
        update.setAmount(new BigDecimal("49.99"));

        assertThatThrownBy(() ->
                service.confirmPayment(1L, update, 42L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void failedPaymentReleasesHold() {
        Ticket ticket = pendingTicket();
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        var response =
                service.failPayment(1L, paymentUpdate(), 42L, false);

        assertThat(response.getStatus())
                .isEqualTo(TicketStatus.PAYMENT_FAILED);
        assertThat(response.getPaymentExpiresAt()).isNull();
        assertThat(response.getPaymentId()).isEqualTo(10L);
    }

    @Test
    void refundCancelsPaidTicketBeforeDeparture() {
        Ticket ticket = pendingTicket();
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setPaymentId(10L);
        ticket.setPaidAt(LocalDateTime.now());
        ticket.setPaymentExpiresAt(null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        var response =
                service.confirmRefund(1L, paymentUpdate(), 42L, false);

        assertThat(response.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(response.getCancelledAt()).isNotNull();
    }

    @Test
    void paidTicketCannotBypassRefundWithNormalCancellation() {
        Ticket ticket = pendingTicket();
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setPaymentId(10L);
        ticket.setPaymentExpiresAt(null);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                service.cancelTicket(1L, 42L, false))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessageContaining("refund");
    }

    private PaymentUpdateRequestDto paymentUpdate() {
        PaymentUpdateRequestDto update = new PaymentUpdateRequestDto();
        update.setPaymentId(10L);
        update.setAmount(new BigDecimal("50.00"));
        return update;
    }

    private Ticket pendingTicket() {
        return Ticket.builder()
                .ticketId(1L)
                .pnrNumber("PNR0000001")
                .userId(42L)
                .routeId(10L)
                .scheduleId(20L)
                .sourceStopId(100L)
                .destinationStopId(200L)
                .serviceDate(LocalDate.now().plusDays(1))
                .departureTime(LocalTime.NOON)
                .passengerCount(2)
                .unitFare(new BigDecimal("25.00"))
                .fareAmount(new BigDecimal("50.00"))
                .status(TicketStatus.PENDING_PAYMENT)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }
}
