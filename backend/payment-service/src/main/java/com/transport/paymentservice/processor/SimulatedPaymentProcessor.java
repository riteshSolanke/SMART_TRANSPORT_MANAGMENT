package com.transport.paymentservice.processor;

import com.transport.paymentservice.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {
    private final boolean successfulOutcome;

    public SimulatedPaymentProcessor(
            @Value("${payment.simulator.outcome:SUCCESS}") String outcome) {
        successfulOutcome = "SUCCESS".equalsIgnoreCase(outcome);
    }

    @Override
    public ProcessorResult charge(
            String transactionReference,
            BigDecimal amount,
            PaymentMethod paymentMethod) {
        return successfulOutcome
                ? ProcessorResult.success()
                : ProcessorResult.failure("Payment was declined by the simulator");
    }

    @Override
    public ProcessorResult refund(
            String transactionReference, BigDecimal amount) {
        return successfulOutcome
                ? ProcessorResult.success()
                : ProcessorResult.failure("Refund was declined by the simulator");
    }
}
