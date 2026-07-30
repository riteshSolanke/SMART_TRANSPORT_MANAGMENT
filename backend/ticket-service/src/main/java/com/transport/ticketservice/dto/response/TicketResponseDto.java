package com.transport.ticketservice.dto.response;


import com.transport.ticketservice.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {
    private Long ticketId;
    private String pnrNumber;
    private Long userId;
    private Long routeId;
    private Long scheduleId;
    private Long sourceStopId;
    private Long destinationStopId;
    private LocalDate serviceDate;
    private LocalTime departureTime;
    private Integer passengerCount;
    private BigDecimal unitFare;
    private BigDecimal fareAmount;
    private Long assignmentId;
    private Long vehicleId;
    private LocalDateTime paymentExpiresAt;
    private Long paymentId;
    private LocalDateTime paidAt;
    private TicketStatus status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime travelDate;

}

