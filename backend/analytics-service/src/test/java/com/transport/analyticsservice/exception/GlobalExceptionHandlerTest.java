package com.transport.analyticsservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void missingRequestParameterReturnsBadRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/analytics/usage");

        var response = handler.handleInvalidRequestParameter(
                mock(MissingServletRequestParameterException.class),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Invalid or missing request parameter");
        assertThat(response.getBody().getPath())
                .isEqualTo("/api/analytics/usage");
    }

    @Test
    void missingResourceReturnsNotFound() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/analytics/missing");

        var response = handler.handleMissingResource(
                mock(NoResourceFoundException.class),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Resource not found");
    }
}
