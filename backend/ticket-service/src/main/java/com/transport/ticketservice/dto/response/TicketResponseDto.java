package com.transport.ticketservice.dto.response;


import com.transport.ticketservice.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private Long sourceStopId;
    private Long destinationStopId;
    private BigDecimal fareAmount;
    private TicketStatus status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime travelDate;

}

