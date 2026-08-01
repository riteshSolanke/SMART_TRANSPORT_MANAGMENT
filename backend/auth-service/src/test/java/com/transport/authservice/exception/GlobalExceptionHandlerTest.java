package com.transport.authservice.exception;

import com.transport.authservice.dto.response.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(mock(MessageSource.class));
        request = new MockHttpServletRequest(
                "GET",
                "/api/auth/login/send-otp");
    }

    @Test
    void unsupportedHttpMethodReturns405InsteadOf500() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleMethodNotSupported(
                        new HttpRequestMethodNotSupportedException("GET"),
                        request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(405);
        assertThat(response.getBody().getMessage()).contains("GET");
    }

    @Test
    void invalidBusinessInputReturns400InsteadOf500() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleBadRequest(
                        new IllegalArgumentException("Invalid staff role"),
                        request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }
}
