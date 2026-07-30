package com.transport.analyticsservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodAnalyticsDto {
    private String paymentMethod;
    private long totalAttempts;
    private long successfulPayments;
    private long failedPayments;
    private long refundedPayments;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
}
