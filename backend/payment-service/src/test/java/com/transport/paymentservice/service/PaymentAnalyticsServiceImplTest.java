package com.transport.paymentservice.service;

import com.transport.paymentservice.enums.PaymentStatus;
import com.transport.paymentservice.repository.PaymentRepository;
import com.transport.paymentservice.repository.projection.PaymentStatusAnalyticsProjection;
import com.transport.paymentservice.service.impl.PaymentAnalyticsServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentAnalyticsServiceImplTest {
    @Test
    void calculatesGrossRefundedAndNetRevenue() {
        PaymentRepository repository = mock(PaymentRepository.class);
        List<PaymentStatusAnalyticsProjection> statusRows = List.of(
                status(PaymentStatus.SUCCESS, 3, "150.00"),
                status(PaymentStatus.REFUNDED, 1, "50.00"),
                status(PaymentStatus.FAILED, 2, "100.00"));
        when(repository.summarizeByStatus(any(), any()))
                .thenReturn(statusRows);
        when(repository.summarizeByMethodAndStatus(any(), any()))
                .thenReturn(List.of());

        var response =
                new PaymentAnalyticsServiceImpl(repository)
                        .summarize(FROM, TO);

        assertThat(response.getTotalAttempts()).isEqualTo(6);
        assertThat(response.getSuccessfulPayments()).isEqualTo(4);
        assertThat(response.getFailedPayments()).isEqualTo(2);
        assertThat(response.getRefundedPayments()).isEqualTo(1);
        assertThat(response.getGrossRevenue())
                .isEqualByComparingTo("200.00");
        assertThat(response.getRefundedAmount())
                .isEqualByComparingTo("50.00");
        assertThat(response.getNetRevenue())
                .isEqualByComparingTo("150.00");
    }

    private PaymentStatusAnalyticsProjection status(
            PaymentStatus status, long count, String amount) {
        PaymentStatusAnalyticsProjection row =
                mock(PaymentStatusAnalyticsProjection.class);
        when(row.getStatus()).thenReturn(status);
        when(row.getPaymentCount()).thenReturn(count);
        when(row.getAmountTotal()).thenReturn(new BigDecimal(amount));
        return row;
    }

    private static final LocalDate FROM =
            LocalDate.of(2026, 7, 1);
    private static final LocalDate TO =
            LocalDate.of(2026, 7, 31);
}
