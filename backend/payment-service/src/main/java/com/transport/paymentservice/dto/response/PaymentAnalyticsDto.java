package com.transport.paymentservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PaymentAnalyticsDto {
    private LocalDate from;
    private LocalDate to;
    private long totalAttempts;
    private long successfulPayments;
    private long failedPayments;
    private long refundedPayments;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
    private List<PaymentMethodAnalyticsDto> paymentMethods;
}
