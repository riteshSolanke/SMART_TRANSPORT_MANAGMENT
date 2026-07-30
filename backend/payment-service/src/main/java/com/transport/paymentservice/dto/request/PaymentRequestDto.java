package com.transport.paymentservice.dto.request;

import com.transport.paymentservice.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDto {
    @NotNull
    @Positive
    private Long ticketId;

    @NotNull
    private PaymentMethod paymentMethod;
}
