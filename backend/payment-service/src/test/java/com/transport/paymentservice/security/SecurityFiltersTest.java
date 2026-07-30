package com.transport.paymentservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityFiltersTest {
    private static final String SECRET =
            "gateway-test-secret-that-is-at-least-32-characters";

    @Test
    void internalFilterRejectsMissingGatewaySecret() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new InternalValidationFilter(SECRET).doFilter(
                new MockHttpServletRequest(),
                response,
                new MockFilterChain());

        assertThat(response.getStatus())
                .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void headerFilterCreatesPassengerAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "PASSENGER");

        new HeaderAuthenticationFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getName()).isEqualTo("42");
        SecurityContextHolder.clearContext();
    }
}
