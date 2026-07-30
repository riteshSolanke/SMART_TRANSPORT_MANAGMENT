package com.transport.paymentservice.dto.response;

import com.transport.paymentservice.enums.PaymentMethod;
import com.transport.paymentservice.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private String transactionReference;
    private Long ticketId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime refundedAt;
}
