package com.transport.paymentservice.service.impl;

import com.transport.paymentservice.dto.response.PaymentAnalyticsDto;
import com.transport.paymentservice.dto.response.PaymentMethodAnalyticsDto;
import com.transport.paymentservice.enums.PaymentMethod;
import com.transport.paymentservice.enums.PaymentStatus;
import com.transport.paymentservice.repository.PaymentRepository;
import com.transport.paymentservice.repository.projection.PaymentMethodAnalyticsProjection;
import com.transport.paymentservice.repository.projection.PaymentStatusAnalyticsProjection;
import com.transport.paymentservice.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsServiceImpl implements PaymentAnalyticsService {
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentAnalyticsDto summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);
        var fromTime = from.atStartOfDay();
        var toExclusive = to.plusDays(1).atStartOfDay();
        List<PaymentStatusAnalyticsProjection> statusRows =
                paymentRepository.summarizeByStatus(fromTime, toExclusive);
        List<PaymentMethodAnalyticsProjection> methodRows =
                paymentRepository.summarizeByMethodAndStatus(
                        fromTime, toExclusive);

        long totalAttempts = statusRows.stream()
                .mapToLong(row -> value(row.getPaymentCount())).sum();
        long successful = count(
                statusRows, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
        long failed = count(statusRows, PaymentStatus.FAILED);
        long refunded = count(statusRows, PaymentStatus.REFUNDED);
        BigDecimal gross = amount(
                statusRows, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
        BigDecimal refunds = amount(statusRows, PaymentStatus.REFUNDED);

        List<PaymentMethodAnalyticsDto> methods =
                Arrays.stream(PaymentMethod.values())
                        .map(method -> summarizeMethod(method, methodRows))
                        .filter(dto -> dto.getTotalAttempts() > 0)
                        .toList();

        return PaymentAnalyticsDto.builder()
                .from(from)
                .to(to)
                .totalAttempts(totalAttempts)
                .successfulPayments(successful)
                .failedPayments(failed)
                .refundedPayments(refunded)
                .grossRevenue(gross)
                .refundedAmount(refunds)
                .netRevenue(gross.subtract(refunds))
                .paymentMethods(methods)
                .build();
    }

    private PaymentMethodAnalyticsDto summarizeMethod(
            PaymentMethod method,
            List<PaymentMethodAnalyticsProjection> rows) {
        List<PaymentMethodAnalyticsProjection> matching = rows.stream()
                .filter(row -> row.getPaymentMethod() == method)
                .toList();
        long attempts = matching.stream()
                .mapToLong(row -> value(row.getPaymentCount())).sum();
        long successful = methodCount(
                matching, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
        long failed = methodCount(matching, PaymentStatus.FAILED);
        long refunded = methodCount(matching, PaymentStatus.REFUNDED);
        BigDecimal gross = methodAmount(
                matching, PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
        BigDecimal refunds = methodAmount(matching, PaymentStatus.REFUNDED);
        return PaymentMethodAnalyticsDto.builder()
                .paymentMethod(method)
                .totalAttempts(attempts)
                .successfulPayments(successful)
                .failedPayments(failed)
                .refundedPayments(refunded)
                .grossRevenue(gross)
                .refundedAmount(refunds)
                .netRevenue(gross.subtract(refunds))
                .build();
    }

    private long count(
            List<PaymentStatusAnalyticsProjection> rows,
            PaymentStatus... statuses) {
        return rows.stream()
                .filter(row -> contains(statuses, row.getStatus()))
                .mapToLong(row -> value(row.getPaymentCount()))
                .sum();
    }

    private BigDecimal amount(
            List<PaymentStatusAnalyticsProjection> rows,
            PaymentStatus... statuses) {
        return rows.stream()
                .filter(row -> contains(statuses, row.getStatus()))
                .map(row -> money(row.getAmountTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long methodCount(
            List<PaymentMethodAnalyticsProjection> rows,
            PaymentStatus... statuses) {
        return rows.stream()
                .filter(row -> contains(statuses, row.getStatus()))
                .mapToLong(row -> value(row.getPaymentCount()))
                .sum();
    }

    private BigDecimal methodAmount(
            List<PaymentMethodAnalyticsProjection> rows,
            PaymentStatus... statuses) {
        return rows.stream()
                .filter(row -> contains(statuses, row.getStatus()))
                .map(row -> money(row.getAmountTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean contains(
            PaymentStatus[] statuses, PaymentStatus candidate) {
        return Arrays.asList(statuses).contains(candidate);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "A valid from/to date range is required");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366) {
            throw new IllegalArgumentException(
                    "Analytics date range cannot exceed 366 days");
        }
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
