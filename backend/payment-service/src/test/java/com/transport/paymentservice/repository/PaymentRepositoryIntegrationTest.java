package com.transport.paymentservice.repository;

import com.transport.paymentservice.entity.Payment;
import com.transport.paymentservice.enums.PaymentMethod;
import com.transport.paymentservice.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "eureka.client.enabled=false"
})
class PaymentRepositoryIntegrationTest {
    @Autowired
    private PaymentRepository repository;

    @Test
    void retrievesPaymentByOwnerIdempotencyAndTicket() {
        Payment saved = repository.save(payment());

        assertThat(repository.findByUserIdAndIdempotencyKey(
                42L, "payment-123")).contains(saved);
        assertThat(repository.findByPaymentIdAndUserId(
                saved.getPaymentId(), 42L)).contains(saved);
        assertThat(repository.findByTicketIdAndUserIdOrderByCreatedAtDesc(
                1L, 42L)).containsExactly(saved);
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setTransactionReference("PAY-REPOSITORY-TEST");
        payment.setTicketId(1L);
        payment.setUserId(42L);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setIdempotencyKey("payment-123");
        payment.setRequestHash("a".repeat(64));
        return payment;
    }
}
