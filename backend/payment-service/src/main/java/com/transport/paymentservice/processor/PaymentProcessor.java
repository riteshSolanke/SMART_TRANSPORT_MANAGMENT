package com.transport.paymentservice.processor;

import com.transport.paymentservice.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentProcessor {
    ProcessorResult charge(
            String transactionReference,
            BigDecimal amount,
            PaymentMethod paymentMethod);

    ProcessorResult refund(
            String transactionReference,
            BigDecimal amount);
}
