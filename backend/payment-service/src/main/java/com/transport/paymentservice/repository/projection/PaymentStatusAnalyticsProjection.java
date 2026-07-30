package com.transport.paymentservice.repository.projection;

import com.transport.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentStatusAnalyticsProjection {
    PaymentStatus getStatus();

    Long getPaymentCount();

    BigDecimal getAmountTotal();
}
