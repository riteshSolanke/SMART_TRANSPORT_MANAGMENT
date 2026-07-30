package com.transport.paymentservice.controller;

import com.transport.paymentservice.dto.request.PaymentRequestDto;
import com.transport.paymentservice.dto.response.ApiResponseDto;
import com.transport.paymentservice.dto.response.PaymentResponseDto;
import com.transport.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDto<PaymentResponseDto> process(
            Authentication authentication,
            @RequestHeader(name = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody PaymentRequestDto request) {
        return ApiResponseDto.success(
                "Payment processed",
                paymentService.process(
                        request,
                        idempotencyKey,
                        userId(authentication),
                        isAdmin(authentication)));
    }

    @GetMapping("/{paymentId}")
    public ApiResponseDto<PaymentResponseDto> get(
            Authentication authentication,
            @PathVariable Long paymentId) {
        return ApiResponseDto.success(paymentService.get(
                paymentId,
                userId(authentication),
                isAdmin(authentication)));
    }

    @GetMapping("/me")
    public ApiResponseDto<List<PaymentResponseDto>> getMine(
            Authentication authentication) {
        return ApiResponseDto.success(
                paymentService.getMine(userId(authentication)));
    }

    @GetMapping("/ticket/{ticketId}")
    public ApiResponseDto<List<PaymentResponseDto>> getForTicket(
            Authentication authentication,
            @PathVariable Long ticketId) {
        return ApiResponseDto.success(paymentService.getForTicket(
                ticketId,
                userId(authentication),
                isAdmin(authentication)));
    }

    @PostMapping("/{paymentId}/refund")
    public ApiResponseDto<PaymentResponseDto> refund(
            Authentication authentication,
            @PathVariable Long paymentId) {
        return ApiResponseDto.success(
                "Payment refunded",
                paymentService.refund(
                        paymentId,
                        userId(authentication),
                        isAdmin(authentication)));
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
