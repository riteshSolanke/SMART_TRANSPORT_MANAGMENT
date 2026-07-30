package com.transport.paymentservice.dto.response;

import com.transport.paymentservice.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentMethodAnalyticsDto {
    private PaymentMethod paymentMethod;
    private long totalAttempts;
    private long successfulPayments;
    private long failedPayments;
    private long refundedPayments;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
}
