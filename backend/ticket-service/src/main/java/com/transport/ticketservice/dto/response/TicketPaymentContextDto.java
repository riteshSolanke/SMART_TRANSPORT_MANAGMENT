package com.transport.ticketservice.dto.response;

import com.transport.ticketservice.enums.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketPaymentContextDto {
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private TicketStatus status;
    private LocalDateTime paymentExpiresAt;
    private LocalDateTime departureAt;
    private Long paymentId;
    private boolean payable;
    private boolean refundable;
}
