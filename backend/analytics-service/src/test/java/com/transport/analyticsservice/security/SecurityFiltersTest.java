package com.transport.analyticsservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityFiltersTest {
    private static final String SECRET =
            "gateway-test-secret-that-is-at-least-32-characters";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void directRequestWithoutGatewaySecretIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new InternalValidationFilter(SECRET).doFilter(
                new MockHttpServletRequest(),
                response,
                new MockFilterChain());

        assertThat(response.getStatus())
                .isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void managerHeadersCreateManagerAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "7");
        request.addHeader("X-User-Role", "TRANSPORT_MANAGER");

        new HeaderAuthenticationFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getName()).isEqualTo("7");
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_TRANSPORT_MANAGER");
    }
}
