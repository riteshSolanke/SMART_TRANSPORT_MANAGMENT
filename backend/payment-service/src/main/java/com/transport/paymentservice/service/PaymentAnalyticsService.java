package com.transport.paymentservice.service;

import com.transport.paymentservice.dto.response.PaymentAnalyticsDto;

import java.time.LocalDate;

public interface PaymentAnalyticsService {
    PaymentAnalyticsDto summarize(LocalDate from, LocalDate to);
}
