package com.transport.paymentservice.exception;

import com.transport.paymentservice.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(
            RuntimeException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({PaymentConflictException.class,
            DataIntegrityViolationException.class})
    ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "The request conflicts with an existing payment"
                : exception.getMessage();
        return build(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            PaymentNotAllowedException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(
            RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof HttpMessageNotReadableException
                ? "Malformed JSON request"
                : exception.getMessage();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    ResponseEntity<ApiErrorResponse> handleProcessingFailure(
            PaymentProcessingException exception, HttpServletRequest request) {
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(TicketServiceUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleServiceUnavailable(
            TicketServiceUnavailableException exception,
            HttpServletRequest request) {
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");
        body.put("errors", errors);
        body.put("timestamp", LocalDateTime.now());
        body.put("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        log.error(
                "Unexpected payment request failure on {}",
                request.getRequestURI(),
                exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .status(status.value())
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .path(request.getRequestURI())
                        .build());
    }
}
