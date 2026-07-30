package com.transport.paymentservice.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TicketPaymentContextDto {
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paymentExpiresAt;
    private LocalDateTime departureAt;
    private Long paymentId;
    private boolean payable;
    private boolean refundable;
}
