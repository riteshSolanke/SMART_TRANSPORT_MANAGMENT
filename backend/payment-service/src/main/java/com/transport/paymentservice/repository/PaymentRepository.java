package com.transport.paymentservice.repository;

import com.transport.paymentservice.entity.Payment;
import com.transport.paymentservice.enums.PaymentStatus;
import com.transport.paymentservice.repository.projection.PaymentMethodAnalyticsProjection;
import com.transport.paymentservice.repository.projection.PaymentStatusAnalyticsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Query("""
            SELECT p.status AS status,
                   COUNT(p) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS amountTotal
            FROM Payment p
            WHERE p.createdAt >= :from
              AND p.createdAt < :toExclusive
            GROUP BY p.status
            """)
    List<PaymentStatusAnalyticsProjection> summarizeByStatus(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            SELECT p.paymentMethod AS paymentMethod,
                   p.status AS status,
                   COUNT(p) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS amountTotal
            FROM Payment p
            WHERE p.createdAt >= :from
              AND p.createdAt < :toExclusive
            GROUP BY p.paymentMethod, p.status
            """)
    List<PaymentMethodAnalyticsProjection> summarizeByMethodAndStatus(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
}
