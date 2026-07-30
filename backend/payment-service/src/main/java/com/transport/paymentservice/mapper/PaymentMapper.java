package com.transport.paymentservice.mapper;

import com.transport.paymentservice.dto.response.PaymentResponseDto;
import com.transport.paymentservice.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMapper {
    public PaymentResponseDto toResponse(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .transactionReference(payment.getTransactionReference())
                .ticketId(payment.getTicketId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .completedAt(payment.getCompletedAt())
                .refundedAt(payment.getRefundedAt())
                .build();
    }

    public List<PaymentResponseDto> toResponse(List<Payment> payments) {
        return payments.stream().map(this::toResponse).toList();
    }
}
