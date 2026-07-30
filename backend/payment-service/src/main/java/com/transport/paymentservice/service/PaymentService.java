package com.transport.paymentservice.service;

import com.transport.paymentservice.dto.request.PaymentRequestDto;
import com.transport.paymentservice.dto.response.PaymentResponseDto;

import java.util.List;

public interface PaymentService {
    PaymentResponseDto process(
            PaymentRequestDto request,
            String idempotencyKey,
            Long authenticatedUserId,
            boolean admin);

    PaymentResponseDto get(
            Long paymentId, Long authenticatedUserId, boolean admin);

    List<PaymentResponseDto> getMine(Long authenticatedUserId);

    List<PaymentResponseDto> getForTicket(
            Long ticketId, Long authenticatedUserId, boolean admin);

    PaymentResponseDto refund(
            Long paymentId, Long authenticatedUserId, boolean admin);
}
