package com.transport.ticketservice.controller;

import com.transport.ticketservice.dto.request.PaymentUpdateRequestDto;
import com.transport.ticketservice.dto.response.ApiResponseDto;
import com.transport.ticketservice.dto.response.TicketPaymentContextDto;
import com.transport.ticketservice.dto.response.TicketResponseDto;
import com.transport.ticketservice.security.PaymentServiceKeyValidator;
import com.transport.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/{ticketId}/payment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
public class TicketPaymentController {
    private final TicketService ticketService;
    private final PaymentServiceKeyValidator paymentServiceKeyValidator;

    @GetMapping("/context")
    public ApiResponseDto<TicketPaymentContextDto> getContext(
            Authentication authentication,
            @PathVariable Long ticketId,
            @RequestHeader(name = "X-Payment-Service-Key", required = false)
            String paymentServiceKey) {
        paymentServiceKeyValidator.validate(paymentServiceKey);
        return ApiResponseDto.success(ticketService.getPaymentContext(
                ticketId, userId(authentication), isAdmin(authentication)));
    }

    @PostMapping("/confirmation")
    public ApiResponseDto<TicketResponseDto> confirm(
            Authentication authentication,
            @PathVariable Long ticketId,
            @RequestHeader(name = "X-Payment-Service-Key", required = false)
            String paymentServiceKey,
            @Valid @RequestBody PaymentUpdateRequestDto request) {
        paymentServiceKeyValidator.validate(paymentServiceKey);
        return ApiResponseDto.success(
                "Ticket payment confirmed",
                ticketService.confirmPayment(
                        ticketId, request,
                        userId(authentication), isAdmin(authentication)));
    }

    @PostMapping("/failure")
    public ApiResponseDto<TicketResponseDto> fail(
            Authentication authentication,
            @PathVariable Long ticketId,
            @RequestHeader(name = "X-Payment-Service-Key", required = false)
            String paymentServiceKey,
            @Valid @RequestBody PaymentUpdateRequestDto request) {
        paymentServiceKeyValidator.validate(paymentServiceKey);
        return ApiResponseDto.success(
                "Ticket payment failure recorded",
                ticketService.failPayment(
                        ticketId, request,
                        userId(authentication), isAdmin(authentication)));
    }

    @PostMapping("/refund")
    public ApiResponseDto<TicketResponseDto> refund(
            Authentication authentication,
            @PathVariable Long ticketId,
            @RequestHeader(name = "X-Payment-Service-Key", required = false)
            String paymentServiceKey,
            @Valid @RequestBody PaymentUpdateRequestDto request) {
        paymentServiceKeyValidator.validate(paymentServiceKey);
        return ApiResponseDto.success(
                "Ticket refund confirmed",
                ticketService.confirmRefund(
                        ticketId, request,
                        userId(authentication), isAdmin(authentication)));
    }

    private Long userId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
