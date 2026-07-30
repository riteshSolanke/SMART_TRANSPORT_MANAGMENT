package com.transport.paymentservice.repository;

import com.transport.paymentservice.entity.Payment;
import com.transport.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByUserIdAndIdempotencyKey(
            Long userId, String idempotencyKey);

    Optional<Payment> findByPaymentIdAndUserId(Long paymentId, Long userId);

    Optional<Payment> findFirstByTicketIdAndStatus(
            Long ticketId, PaymentStatus status);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<Payment> findByTicketIdAndUserIdOrderByCreatedAtDesc(
            Long ticketId, Long userId);

    boolean existsByTransactionReference(String transactionReference);
}
