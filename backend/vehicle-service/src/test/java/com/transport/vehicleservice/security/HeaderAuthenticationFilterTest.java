package com.transport.vehicleservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderAuthenticationFilterTest {
    private final HeaderAuthenticationFilter filter =
            new HeaderAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidatedGatewayIdentity() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/vehicles/1");
        request.addHeader("X-User-Id", "42");
        request.addHeader("X-User-Role", "DISPATCHER");

        filter.doFilter(request, new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("42");
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities()).extracting("authority")
                .containsExactly("ROLE_DISPATCHER");
    }

    @Test
    void rejectsInvalidIdentity() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/vehicles/1");
        request.addHeader("X-User-Id", "-1");
        request.addHeader("X-User-Role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }
}
