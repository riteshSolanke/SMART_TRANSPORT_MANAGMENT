package com.transport.paymentservice.service.impl;

import com.transport.paymentservice.client.TicketServiceClient;
import com.transport.paymentservice.dto.request.PaymentRequestDto;
import com.transport.paymentservice.dto.request.TicketPaymentUpdateDto;
import com.transport.paymentservice.dto.response.ApiResponseDto;
import com.transport.paymentservice.dto.response.PaymentResponseDto;
import com.transport.paymentservice.dto.response.TicketPaymentContextDto;
import com.transport.paymentservice.entity.Payment;
import com.transport.paymentservice.enums.PaymentStatus;
import com.transport.paymentservice.exception.PaymentConflictException;
import com.transport.paymentservice.exception.PaymentNotAllowedException;
import com.transport.paymentservice.exception.PaymentNotFoundException;
import com.transport.paymentservice.exception.PaymentProcessingException;
import com.transport.paymentservice.exception.TicketServiceUnavailableException;
import com.transport.paymentservice.mapper.PaymentMapper;
import com.transport.paymentservice.processor.PaymentProcessor;
import com.transport.paymentservice.processor.ProcessorResult;
import com.transport.paymentservice.repository.PaymentRepository;
import com.transport.paymentservice.service.PaymentService;
import com.transport.paymentservice.util.PaymentRequestHasher;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final TicketServiceClient ticketServiceClient;
    private final PaymentProcessor paymentProcessor;
    private final PaymentRequestHasher requestHasher;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponseDto process(
            PaymentRequestDto request,
            String rawIdempotencyKey,
            Long authenticatedUserId,
            boolean admin) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        String requestHash = requestHasher.hash(authenticatedUserId, request);

        Payment previous = paymentRepository
                .findByUserIdAndIdempotencyKey(
                        authenticatedUserId, idempotencyKey)
                .orElse(null);
        if (previous != null) {
            if (!requestHash.equals(previous.getRequestHash())) {
                throw new PaymentConflictException(
                        "Idempotency key was already used for a different payment request");
            }
            return paymentMapper.toResponse(previous);
        }

        TicketPaymentContextDto context =
                getTicketContext(
                        request.getTicketId(), authenticatedUserId, admin);
        validatePayableTicket(
                context, request.getTicketId(), authenticatedUserId, admin);

        Payment completed = paymentRepository.findFirstByTicketIdAndStatus(
                request.getTicketId(), PaymentStatus.SUCCESS).orElse(null);
        if (completed != null) {
            verifyAccess(completed, authenticatedUserId, admin);
            return paymentMapper.toResponse(completed);
        }

        BigDecimal amount =
                context.getAmount().setScale(2, RoundingMode.HALF_UP);
        Payment payment = new Payment();
        payment.setTransactionReference(generateTransactionReference());
        payment.setTicketId(request.getTicketId());
        payment.setUserId(authenticatedUserId);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setRequestHash(requestHash);
        payment = paymentRepository.saveAndFlush(payment);
        Payment persistedPayment = payment;

        ProcessorResult result = paymentProcessor.charge(
                payment.getTransactionReference(),
                payment.getAmount(),
                payment.getPaymentMethod());
        TicketPaymentUpdateDto update = new TicketPaymentUpdateDto(
                payment.getPaymentId(), payment.getAmount());
        if (result.successful()) {
            requireSuccessfulCallback(
                    () -> ticketServiceClient.confirmPayment(
                            persistedPayment.getTicketId(),
                            identity(authenticatedUserId),
                            role(admin),
                            update));
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(LocalDateTime.now());
        } else {
            requireSuccessfulCallback(
                    () -> ticketServiceClient.failPayment(
                            persistedPayment.getTicketId(),
                            identity(authenticatedUserId),
                            role(admin),
                            update));
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(safeFailureReason(result.failureReason()));
        }
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto get(
            Long paymentId, Long authenticatedUserId, boolean admin) {
        Payment payment = requirePayment(paymentId);
        verifyAccess(payment, authenticatedUserId, admin);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getMine(Long authenticatedUserId) {
        return paymentMapper.toResponse(
                paymentRepository.findByUserIdOrderByCreatedAtDesc(
                        authenticatedUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getForTicket(
            Long ticketId, Long authenticatedUserId, boolean admin) {
        List<Payment> payments = admin
                ? paymentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                : paymentRepository
                        .findByTicketIdAndUserIdOrderByCreatedAtDesc(
                                ticketId, authenticatedUserId);
        return paymentMapper.toResponse(payments);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponseDto refund(
            Long paymentId, Long authenticatedUserId, boolean admin) {
        Payment payment = requirePayment(paymentId);
        verifyAccess(payment, authenticatedUserId, admin);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return paymentMapper.toResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentNotAllowedException(
                    "Only successful payments can be refunded");
        }

        TicketPaymentContextDto context =
                getTicketContext(
                        payment.getTicketId(), authenticatedUserId, admin);
        if (!context.isRefundable()
                || context.getPaymentId() == null
                || !paymentId.equals(context.getPaymentId())
                || context.getAmount() == null
                || payment.getAmount().compareTo(context.getAmount()) != 0) {
            throw new PaymentNotAllowedException(
                    "This ticket is not eligible for a refund");
        }

        ProcessorResult result = paymentProcessor.refund(
                payment.getTransactionReference(), payment.getAmount());
        if (!result.successful()) {
            throw new PaymentProcessingException(
                    safeFailureReason(result.failureReason()));
        }
        TicketPaymentUpdateDto update = new TicketPaymentUpdateDto(
                payment.getPaymentId(), payment.getAmount());
        requireSuccessfulCallback(() -> ticketServiceClient.confirmRefund(
                payment.getTicketId(),
                identity(authenticatedUserId),
                role(admin),
                update));
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    private Payment requirePayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with id: " + paymentId));
    }

    private TicketPaymentContextDto getTicketContext(
            Long ticketId, Long authenticatedUserId, boolean admin) {
        try {
            ApiResponseDto<TicketPaymentContextDto> response =
                    ticketServiceClient.getPaymentContext(
                            ticketId,
                            identity(authenticatedUserId),
                            role(admin));
            TicketPaymentContextDto context =
                    response == null || !response.isSuccess()
                            ? null : response.getData();
            if (context == null) {
                throw new TicketServiceUnavailableException(
                        "Ticket service returned an invalid payment context");
            }
            return context;
        } catch (FeignException exception) {
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new PaymentNotAllowedException(
                        "The ticket could not be approved for payment");
            }
            throw unavailable(exception);
        } catch (PaymentNotAllowedException
                 | TicketServiceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            FeignException feignException = findFeignCause(exception);
            if (feignException != null
                    && feignException.status() >= 400
                    && feignException.status() < 500) {
                throw new PaymentNotAllowedException(
                        "The ticket could not be approved for payment");
            }
            throw unavailable(exception);
        }
    }

    private void validatePayableTicket(
            TicketPaymentContextDto context,
            Long ticketId,
            Long authenticatedUserId,
            boolean admin) {
        if (!ticketId.equals(context.getTicketId())
                || context.getUserId() == null
                || context.getAmount() == null
                || context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentNotAllowedException(
                    "Ticket service returned invalid payment details");
        }
        if (!admin && !authenticatedUserId.equals(context.getUserId())) {
            throw new AccessDeniedException(
                    "You can only pay for your own ticket");
        }
        if (!context.isPayable()) {
            throw new PaymentNotAllowedException(
                    "The ticket is not awaiting payment or its hold has expired");
        }
    }

    private void requireSuccessfulCallback(Callback callback) {
        try {
            ApiResponseDto<Object> response = callback.invoke();
            if (response == null || !response.isSuccess()) {
                throw new TicketServiceUnavailableException(
                        "Ticket service did not accept the payment update");
            }
        } catch (FeignException exception) {
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new PaymentNotAllowedException(
                        "Ticket service rejected the payment update");
            }
            throw unavailable(exception);
        } catch (PaymentNotAllowedException
                 | TicketServiceUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            FeignException feignException = findFeignCause(exception);
            if (feignException != null
                    && feignException.status() >= 400
                    && feignException.status() < 500) {
                throw new PaymentNotAllowedException(
                        "Ticket service rejected the payment update");
            }
            throw unavailable(exception);
        }
    }

    private FeignException findFeignCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FeignException feignException) {
                return feignException;
            }
            current = current.getCause();
        }
        return null;
    }

    private TicketServiceUnavailableException unavailable(Exception cause) {
        return new TicketServiceUnavailableException(
                "Ticket service is temporarily unavailable");
    }

    private String identity(Long authenticatedUserId) {
        return authenticatedUserId.toString();
    }

    private String role(boolean admin) {
        return admin ? "ADMIN" : "PASSENGER";
    }

    private void verifyAccess(
            Payment payment, Long authenticatedUserId, boolean admin) {
        if (!admin && !payment.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException(
                    "You can only access your own payments");
        }
    }

    private String normalizeIdempotencyKey(String rawKey) {
        String key = rawKey == null ? "" : rawKey.trim();
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must be 8 to 64 URL-safe characters");
        }
        return key;
    }

    private String generateTransactionReference() {
        String reference;
        do {
            reference = "PAY-" + UUID.randomUUID()
                    .toString().replace("-", "").toUpperCase();
        } while (paymentRepository.existsByTransactionReference(reference));
        return reference;
    }

    private String safeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Payment processor declined the request";
        }
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }

    @FunctionalInterface
    private interface Callback {
        ApiResponseDto<Object> invoke();
    }
}
