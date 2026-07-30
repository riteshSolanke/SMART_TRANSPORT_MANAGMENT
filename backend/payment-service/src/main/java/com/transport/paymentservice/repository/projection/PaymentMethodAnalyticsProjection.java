package com.transport.paymentservice.repository.projection;

import com.transport.paymentservice.enums.PaymentMethod;
import com.transport.paymentservice.enums.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentMethodAnalyticsProjection {
    PaymentMethod getPaymentMethod();

    PaymentStatus getStatus();

    Long getPaymentCount();

    BigDecimal getAmountTotal();
}
