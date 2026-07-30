package com.transport.paymentservice.service;

import com.transport.paymentservice.client.TicketServiceClient;
import com.transport.paymentservice.dto.request.PaymentRequestDto;
import com.transport.paymentservice.dto.request.TicketPaymentUpdateDto;
import com.transport.paymentservice.dto.response.ApiResponseDto;
import com.transport.paymentservice.dto.response.TicketPaymentContextDto;
import com.transport.paymentservice.entity.Payment;
import com.transport.paymentservice.enums.PaymentMethod;
import com.transport.paymentservice.enums.PaymentStatus;
import com.transport.paymentservice.exception.PaymentConflictException;
import com.transport.paymentservice.exception.PaymentNotAllowedException;
import com.transport.paymentservice.mapper.PaymentMapper;
import com.transport.paymentservice.processor.PaymentProcessor;
import com.transport.paymentservice.processor.ProcessorResult;
import com.transport.paymentservice.repository.PaymentRepository;
import com.transport.paymentservice.service.impl.PaymentServiceImpl;
import com.transport.paymentservice.util.PaymentRequestHasher;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock
    private PaymentRepository repository;
    @Mock
    private TicketServiceClient ticketClient;
    @Mock
    private PaymentProcessor processor;

    private final PaymentRequestHasher hasher = new PaymentRequestHasher();
    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                repository,
                new PaymentMapper(),
                ticketClient,
                processor,
                hasher);
        lenient().when(repository.saveAndFlush(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setPaymentId(10L);
                    return payment;
                });
        lenient().when(repository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void successfulChargeConfirmsTicket() {
        PaymentRequestDto request = request();
        stubPayableContext();
        when(processor.charge(any(), any(), any()))
                .thenReturn(ProcessorResult.success());
        when(ticketClient.confirmPayment(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class)))
                .thenReturn(ApiResponseDto.success(new Object()));

        var response =
                service.process(request, "payment-123", 42L, false);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualByComparingTo("50.00");
        assertThat(response.getCompletedAt()).isNotNull();
        verify(ticketClient).confirmPayment(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class));
        verify(ticketClient, never()).failPayment(
                org.mockito.ArgumentMatchers.anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void declinedChargeIsRecordedAndReleasesTicketHold() {
        PaymentRequestDto request = request();
        stubPayableContext();
        when(processor.charge(any(), any(), any()))
                .thenReturn(ProcessorResult.failure("Declined"));
        when(ticketClient.failPayment(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class)))
                .thenReturn(ApiResponseDto.success(new Object()));

        var response =
                service.process(request, "payment-123", 42L, false);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getFailureReason()).isEqualTo("Declined");
        verify(ticketClient).failPayment(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class));
    }

    @Test
    void sameIdempotencyKeyAndRequestReturnsPreviousResult() {
        PaymentRequestDto request = request();
        Payment previous = payment(PaymentStatus.SUCCESS);
        previous.setRequestHash(hasher.hash(42L, request));
        when(repository.findByUserIdAndIdempotencyKey(
                42L, "payment-123")).thenReturn(Optional.of(previous));

        var response =
                service.process(request, "payment-123", 42L, false);

        assertThat(response.getPaymentId()).isEqualTo(10L);
        verify(ticketClient, never()).getPaymentContext(
                any(), anyString(), anyString());
        verify(processor, never()).charge(any(), any(), any());
    }

    @Test
    void reusedIdempotencyKeyWithDifferentRequestIsRejected() {
        Payment previous = payment(PaymentStatus.SUCCESS);
        previous.setRequestHash("different");
        when(repository.findByUserIdAndIdempotencyKey(
                42L, "payment-123")).thenReturn(Optional.of(previous));

        assertThatThrownBy(() ->
                service.process(request(), "payment-123", 42L, false))
                .isInstanceOf(PaymentConflictException.class);
    }

    @Test
    void passengerCannotPayForAnotherUsersTicket() {
        TicketPaymentContextDto context = payableContext();
        context.setUserId(99L);
        when(ticketClient.getPaymentContext(1L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(context));

        assertThatThrownBy(() ->
                service.process(request(), "payment-123", 42L, false))
                .isInstanceOf(AccessDeniedException.class);
        verify(processor, never()).charge(any(), any(), any());
    }

    @Test
    void wrappedTicketNotFoundIsReportedAsInvalidPaymentRequest() {
        FeignException downstream = org.mockito.Mockito.mock(
                FeignException.class);
        when(downstream.status()).thenReturn(404);
        RuntimeException wrapped = new RuntimeException(downstream);
        when(ticketClient.getPaymentContext(1L, "42", "PASSENGER"))
                .thenThrow(wrapped);

        assertThatThrownBy(() ->
                service.process(request(), "payment-123", 42L, false))
                .isInstanceOf(PaymentNotAllowedException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void successfulPaymentCanBeRefunded() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(repository.findById(10L)).thenReturn(Optional.of(payment));
        TicketPaymentContextDto context = payableContext();
        context.setPayable(false);
        context.setRefundable(true);
        context.setPaymentId(10L);
        when(ticketClient.getPaymentContext(1L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(context));
        when(processor.refund(any(), any()))
                .thenReturn(ProcessorResult.success());
        when(ticketClient.confirmRefund(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class)))
                .thenReturn(ApiResponseDto.success(new Object()));

        var response = service.refund(10L, 42L, false);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getRefundedAt()).isNotNull();
        verify(ticketClient).confirmRefund(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("42"),
                org.mockito.ArgumentMatchers.eq("PASSENGER"),
                any(TicketPaymentUpdateDto.class));
    }

    @Test
    void nonRefundableTicketIsRejectedBeforeProcessorCall() {
        when(repository.findById(10L))
                .thenReturn(Optional.of(payment(PaymentStatus.SUCCESS)));
        TicketPaymentContextDto context = payableContext();
        context.setPayable(false);
        context.setRefundable(false);
        context.setPaymentId(10L);
        when(ticketClient.getPaymentContext(1L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(context));

        assertThatThrownBy(() -> service.refund(10L, 42L, false))
                .isInstanceOf(PaymentNotAllowedException.class);
        verify(processor, never()).refund(any(), any());
    }

    private void stubPayableContext() {
        when(ticketClient.getPaymentContext(1L, "42", "PASSENGER"))
                .thenReturn(ApiResponseDto.success(payableContext()));
        when(repository.findFirstByTicketIdAndStatus(
                1L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(repository.existsByTransactionReference(any()))
                .thenReturn(false);
    }

    private TicketPaymentContextDto payableContext() {
        TicketPaymentContextDto context = new TicketPaymentContextDto();
        context.setTicketId(1L);
        context.setUserId(42L);
        context.setAmount(new BigDecimal("50.00"));
        context.setStatus("PENDING_PAYMENT");
        context.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(5));
        context.setPayable(true);
        return context;
    }

    private PaymentRequestDto request() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setTicketId(1L);
        request.setPaymentMethod(PaymentMethod.UPI);
        return request;
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentId(10L);
        payment.setTransactionReference("PAY-TEST");
        payment.setTicketId(1L);
        payment.setUserId(42L);
        payment.setAmount(new BigDecimal("50.00"));
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setStatus(status);
        payment.setIdempotencyKey("payment-123");
        payment.setRequestHash("a".repeat(64));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }
}
