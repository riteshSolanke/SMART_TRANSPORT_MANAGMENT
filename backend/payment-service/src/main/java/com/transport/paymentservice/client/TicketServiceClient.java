package com.transport.paymentservice.client;

import com.transport.paymentservice.config.FeignConfig;
import com.transport.paymentservice.dto.request.TicketPaymentUpdateDto;
import com.transport.paymentservice.dto.response.ApiResponseDto;
import com.transport.paymentservice.dto.response.TicketPaymentContextDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ticket-service", configuration = FeignConfig.class)
public interface TicketServiceClient {
    @GetMapping("/api/tickets/{ticketId}/payment/context")
    ApiResponseDto<TicketPaymentContextDto> getPaymentContext(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role);

    @PostMapping("/api/tickets/{ticketId}/payment/confirmation")
    ApiResponseDto<Object> confirmPayment(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody TicketPaymentUpdateDto request);

    @PostMapping("/api/tickets/{ticketId}/payment/failure")
    ApiResponseDto<Object> failPayment(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody TicketPaymentUpdateDto request);

    @PostMapping("/api/tickets/{ticketId}/payment/refund")
    ApiResponseDto<Object> confirmRefund(
            @PathVariable("ticketId") Long ticketId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody TicketPaymentUpdateDto request);
}
