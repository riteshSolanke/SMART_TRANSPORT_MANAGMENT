package com.transport.paymentservice.controller;

import com.transport.paymentservice.dto.response.ApiResponseDto;
import com.transport.paymentservice.dto.response.PaymentAnalyticsDto;
import com.transport.paymentservice.security.AnalyticsServiceKeyValidator;
import com.transport.paymentservice.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments/internal/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TRANSPORT_MANAGER','ADMIN')")
public class PaymentAnalyticsController {
    private final PaymentAnalyticsService analyticsService;
    private final AnalyticsServiceKeyValidator keyValidator;

    @GetMapping
    public ApiResponseDto<PaymentAnalyticsDto> summarize(
            @RequestHeader(
                    name = "X-Analytics-Service-Key", required = false)
            String analyticsKey,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        keyValidator.validate(analyticsKey);
        return ApiResponseDto.success(analyticsService.summarize(from, to));
    }
}
